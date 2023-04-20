package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Authorization;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import it.pagopa.pm.gateway.utils.VposPatchUtils;
import lombok.NoArgsConstructor;
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
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;

@Service
@Slf4j
@NoArgsConstructor
public class CcResumeStep2Service {

    private String vposUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private VPosResponseUtils vPosResponseUtils;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private VposPatchUtils vposPatchUtils;

    @Autowired
    public CcResumeStep2Service(@Value("${vpos.requestUrl}") String vposUrl, PaymentRequestRepository paymentRequestRepository,
                                VPosRequestUtils vPosRequestUtils, VPosResponseUtils vPosResponseUtils,
                                HttpClient httpClient, ObjectMapper objectMapper, VposPatchUtils vposPatchUtils) {
        this.vposUrl = vposUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.vPosResponseUtils = vPosResponseUtils;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.vposPatchUtils = vposPatchUtils;
    }

    public void startResumeStep2(String requestId) {
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

        processResume(entity, requestId);
    }

    private void processResume(PaymentRequestEntity entity, String requestId) {
        String responseType = entity.getResponseType();
        String correlationId = entity.getCorrelationId();
        try {
            StepZeroRequest stepZeroRequest = objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class);
            stepZeroRequest.setIsFirstPayment(false);
            if (Objects.nonNull(responseType) && responseType.equalsIgnoreCase(ThreeDS2ResponseTypeEnum.CHALLENGE.name())) {
                Map<String, String> params = vPosRequestUtils.buildStepTwoRequestParams(stepZeroRequest, correlationId);
                executeStep2(params, entity, stepZeroRequest);
            }
        } catch (Exception e) {
            log.error("error during execution of resume for requestId {}", requestId, e);
        }
    }

    @Async
    private void executeStep2(Map<String, String> params, PaymentRequestEntity entity, StepZeroRequest request) {
        try {
            String requestId = entity.getGuid();
            log.info("Calling VPOS - Step 2 - for requestId: " + requestId);
            HttpClientResponse clientResponse = callVPos(params);
            ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), request);
            log.info("Result code from VPOS - Step 2 - for RequestId {} is {}", requestId, response.getResultCode());
            if (isStepTwoResultCodeOk(response, entity)) {
                executeAccount(entity, request);
            }

            vposPatchUtils.executePatchTransaction(entity);
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

    private boolean isStepTwoResultCodeOk(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = entity.getResponseType();
        String correlationId = entity.getCorrelationId();
        String errorCode = StringUtils.EMPTY;
        String rrn = entity.getRrn();
        boolean isToAccount = false;
        if (RESULT_CODE_AUTHORIZED.equals(resultCode)) {
            ThreeDS2Authorization authorizedResponse = ((ThreeDS2Authorization) response.getThreeDS2ResponseElement());
            responseType = response.getResponseType().name();
            isToAccount = true;
            correlationId = authorizedResponse.getTransactionId();
            rrn = authorizedResponse.getRrn();
        } else {
            log.error("Error resultCode {} from Vpos for requestId {}", resultCode, entity.getGuid());
            status = DENIED.name();
            errorCode = resultCode;
        }
        entity.setCorrelationId(correlationId);
        entity.setStatus(status);
        entity.setResponseType(responseType);
        entity.setErrorCode(errorCode);
        entity.setRrn(rrn);
        paymentRequestRepository.save(entity);
        return isToAccount;
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
            log.error("{}{}", GENERIC_ERROR_MSG, entity.getIdTransaction(), e);
        }
    }

    private void checkAccountResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = AUTHORIZED.name();
        String errorCode = StringUtils.EMPTY;
        boolean authorizationOutcome = resultCode.equals(RESULT_CODE_AUTHORIZED);
        if (!authorizationOutcome) {
            status = DENIED.name();
            errorCode = resultCode;
        }
        entity.setAuthorizationCode(response.getAuthorizationNumber());
        entity.setAuthorizationOutcome(authorizationOutcome);
        entity.setStatus(status);
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        log.info("END - Vpos Request Payment Account for requestId {} - resultCode: {} ", entity.getGuid(), resultCode);
    }
}
