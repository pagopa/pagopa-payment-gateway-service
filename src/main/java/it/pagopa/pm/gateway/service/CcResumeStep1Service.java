package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.CreditCardResumeRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.transaction.UpdateAuthRequest;
import it.pagopa.pm.gateway.dto.vpos.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pm.gateway.constant.Messages.GENERIC_ERROR_MSG;
import static it.pagopa.pm.gateway.constant.Messages.PATCH_CLOSE_PAYMENT_ERROR;
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_AUTHORIZED;
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_CHALLENGE;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;

@Service
@Slf4j
public class CcResumeStep1Service {
    private static final String CREQ_QUERY_PARAM = "?creq=";

    @Value("${vpos.requestUrl}")
    private String vposUrl;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private EcommerceClient ecommerceClient;

    @Autowired
    private VPosRequestUtils vPosRequestUtils;

    @Autowired
    private VPosResponseUtils vPosResponseUtils;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientsConfig clientsConfig;

    public void startResumeStep1(CreditCardResumeRequest request, String requestId) {
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error("No CreditCard request entity has been found for requestId: " + requestId);
            return;
        }

        if (Objects.nonNull(entity.getAuthorizationOutcome())) {
            log.warn(String.format("requestId %s already processed", requestId));
            entity.setErrorMessage("requestId already processed");
            return;
        }
        processResume(request, entity, requestId);
    }

    private void processResume(CreditCardResumeRequest request, PaymentRequestEntity entity, String requestId) {
        String methodCompleted = request.getMethodCompleted();
        String responseType = entity.getResponseType();
        String correlationId = entity.getCorrelationId();
        try {
            StepZeroRequest stepZeroRequest = objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class);
            stepZeroRequest.setIsFirstPayment(false);
            MethodCompletedEnum methodCompletedEnum = MethodCompletedEnum.valueOf(methodCompleted);
            if (Objects.nonNull(responseType) && responseType.equalsIgnoreCase(ThreeDS2ResponseTypeEnum.METHOD.name())) {
                Map<String, String> params = vPosRequestUtils.buildStepOneRequestParams(methodCompletedEnum, stepZeroRequest, correlationId);
                executeStep1(params, entity, stepZeroRequest);
            }
        } catch (Exception e) {
            log.error("error during execution of resume for requestId {}", requestId, e);
        }
    }

    @Async
    private void executeStep1(Map<String, String> params, PaymentRequestEntity entity, StepZeroRequest request) {
        try {
            String requestId = entity.getGuid();
            log.info("Calling VPOS - Step 1 - for requestId: " + requestId);
            HttpClientResponse clientResponse = callVPos(params);
            ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), request);
            if (isStepOneResultCodeOk(response, entity)) {
                executeAccount(entity, request);
            }
        } catch (Exception e) {
            log.error("{}{}", GENERIC_ERROR_MSG, entity.getIdTransaction(), e);
        }
    }

    private HttpClientResponse callVPos(Map<String, String> params) throws IOException {
        HttpClientResponse clientResponse = httpClient.post(vposUrl, ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), params);
        if (clientResponse.getStatus() != HttpStatus.OK.value()) {
            log.error("HTTP Response Status: {}", clientResponse.getStatus());
            throw new IOException("Non-ok response from VPos. HTTP status: " + clientResponse.getStatus());
        }
        return clientResponse;
    }

    private boolean isStepOneResultCodeOk(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = entity.getResponseType();
        String responseVposUrl = StringUtils.EMPTY;
        String correlationId = entity.getCorrelationId();
        String errorCode = StringUtils.EMPTY;
        boolean isToAccount = false;
        ThreeDS2ResponseElement threeDS2ResponseElement = response.getThreeDS2ResponseElement();
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                responseType = response.getResponseType().name();
                isToAccount = true;
                correlationId = ((ThreeDS2Authorization) threeDS2ResponseElement).getTransactionId();
                break;
            case RESULT_CODE_CHALLENGE:
                ThreeDS2Challenge challengeResponse = (ThreeDS2Challenge) threeDS2ResponseElement;
                responseType = response.getResponseType().name();
                responseVposUrl = getChallengeUrl(challengeResponse);
                correlationId = challengeResponse.getThreeDSTransId();
                break;
            default:
                log.error("Error resultCode {} from Vpos for requestId {}", resultCode, entity.getGuid());
                errorCode = resultCode;
                status = DENIED.name();
        }
        entity.setCorrelationId(correlationId);
        entity.setStatus(status);
        entity.setAuthorizationUrl(responseVposUrl);
        entity.setResponseType(responseType);
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        return isToAccount;
    }

    private String getChallengeUrl(ThreeDS2Challenge threeDS2Challenge) {
        String url = threeDS2Challenge.getAcsUrl();
        String creq = threeDS2Challenge.getCReq();
        return StringUtils.join(url, CREQ_QUERY_PARAM, creq);
    }

    private void executeAccount(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Accounting - for requestId: {}", entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildAccountingRequestParams(pgsRequest, entity.getCorrelationId());
            HttpClientResponse clientResponse = callVPos(params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkAccountResultCode(response, entity);
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + " stackTrace: " + Arrays.toString(e.getStackTrace()));
        }
        executePatchTransaction(entity);
    }

    private void checkAccountResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = AUTHORIZED.name();
        String errorCode = StringUtils.EMPTY;
        boolean authorizationOutcome = true;
        if (!resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            status = DENIED.name();
            authorizationOutcome = false;
            errorCode = resultCode;
        }
        entity.setAuthorizationCode(response.getAuthorizationNumber());
        entity.setAuthorizationOutcome(authorizationOutcome);
        entity.setStatus(status);
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        log.info("END - XPay Request Payment Account for requestId {}", entity.getGuid());
    }

    private void executePatchTransaction(PaymentRequestEntity entity) {
        String requestId = entity.getGuid();
        log.info("START - PATCH updateTransaction for requestId: {}", requestId);
        AuthResultEnum authResult = entity.getStatus().equals(AUTHORIZED.name()) ? AuthResultEnum.OK : AuthResultEnum.KO;
        String authCode = entity.getAuthorizationCode();
        UpdateAuthRequest patchRequest = new UpdateAuthRequest(authResult, authCode);
        try {
            ClientConfig clientConfig = clientsConfig.getByKey(entity.getClientId());
            TransactionInfo patchResponse = ecommerceClient.callPatchTransaction(patchRequest, entity.getIdTransaction(), clientConfig);
            log.info("Response from PATCH updateTransaction for requestId {} is {}", requestId, patchResponse.toString());
        } catch (Exception e) {
            log.error(PATCH_CLOSE_PAYMENT_ERROR + requestId, e);
            log.info("Refunding payment with requestId: " + requestId);
        }
    }
}
