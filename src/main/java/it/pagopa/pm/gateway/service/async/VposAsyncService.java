package it.pagopa.pm.gateway.service.async;

import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.vpos.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.EcommercePatchUtils;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static it.pagopa.pm.gateway.constant.Messages.GENERIC_ERROR_MSG;
import static it.pagopa.pm.gateway.constant.VposConstant.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;

@Service
@Slf4j
@NoArgsConstructor
public class VposAsyncService {

    private static final String CAUSE = " cause: ";
    private static final List<String> METHOD_CHALLENGE_CODES = Arrays.asList("25", "26");
    private String vposUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private VPosResponseUtils vPosResponseUtils;
    private HttpClient httpClient;
    private EcommercePatchUtils ecommercePatchUtils;

    @Autowired
    public VposAsyncService(@Value("${vpos.requestUrl}") String vposUrl, PaymentRequestRepository paymentRequestRepository,
                            VPosRequestUtils vPosRequestUtils, VPosResponseUtils vPosResponseUtils,
                            HttpClient httpClient, EcommercePatchUtils ecommercePatchUtils) {
        this.vposUrl = vposUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.vPosResponseUtils = vPosResponseUtils;
        this.httpClient = httpClient;
        this.ecommercePatchUtils = ecommercePatchUtils;
    }

    @Async
    public void executeStepZeroAuth(Map<String, String> params, PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            String requestId = entity.getGuid();
            log.info("Calling VPOS - Step 0 - for requestId: " + requestId);
            HttpClientResponse clientResponse = httpClient.callVPos(vposUrl,params);
            ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            log.info("Result code from VPOS - Step 0 - for RequestId {} is {}", requestId, response.getResultCode());
            boolean toAccount = checkResultCode(response, entity);
            if (toAccount) {
                String authNumber = ((ThreeDS2Authorization) response.getThreeDS2ResponseElement()).getAuthorizationNumber();
                executeAccount(entity, pgsRequest, authNumber);
            }

            //If the resultCode is 25 or 26, the PATCH is not called
            if (!METHOD_CHALLENGE_CODES.contains(response.getResultCode())) {
                ecommercePatchUtils.executePatchTransaction(entity);
            }
        } catch (Exception e) {
            log.error("{}{}{}{} - {}",GENERIC_ERROR_MSG,entity.getIdTransaction(),CAUSE,e.getCause(), e.getMessage(), e);
        }
    }

    private void executeAccount(PaymentRequestEntity entity, StepZeroRequest pgsRequest, String authNumber) {
        try {
            log.info("Calling VPOS - Accounting - for requestId: " + entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildAccountingRequestParams(pgsRequest, entity.getCorrelationId());
            HttpClientResponse clientResponse = httpClient.callVPos(vposUrl,params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkAccountResultCode(response, entity, authNumber);
        } catch (Exception e) {
            log.error("{}{}", GENERIC_ERROR_MSG, entity.getIdTransaction(), e);
        }
    }

    private boolean checkResultCode(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = "ERROR";
        String correlationId = StringUtils.EMPTY;
        String responseVposUrl = StringUtils.EMPTY;
        String errorCode = StringUtils.EMPTY;
        String rrn = StringUtils.EMPTY;
        boolean isToAccount = false;
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                ThreeDS2Authorization authorizedResponse = ((ThreeDS2Authorization) response.getThreeDS2ResponseElement());
                responseType = response.getResponseType().name();
                isToAccount = true;
                correlationId = authorizedResponse.getTransactionId();
                rrn = authorizedResponse.getRrn();
                break;
            case RESULT_CODE_METHOD:
                ThreeDS2Method methodResponse = (ThreeDS2Method) response.getThreeDS2ResponseElement();
                responseType = response.getResponseType().name();
                responseVposUrl = methodResponse.getThreeDSMethodUrl();
                correlationId = methodResponse.getThreeDSTransId();
                break;
            case RESULT_CODE_CHALLENGE:
                ThreeDS2Challenge challengeResponse = ((ThreeDS2Challenge) response.getThreeDS2ResponseElement());
                responseType = response.getResponseType().name();
                responseVposUrl = getChallengeUrl(challengeResponse);
                correlationId = (challengeResponse.getThreeDSTransId());
                break;
            default:
                log.error(String.format("Error resultCode %s from Vpos for requestId %s", resultCode, entity.getGuid()));
                errorCode = resultCode;
                status = DENIED.name();
        }
        entity.setCorrelationId(correlationId);
        entity.setStatus(status);
        entity.setAuthorizationUrl(responseVposUrl);
        entity.setResponseType(responseType);
        entity.setErrorCode(errorCode);
        entity.setRrn(rrn);
        paymentRequestRepository.save(entity);
        return isToAccount;
    }

    private String getChallengeUrl(ThreeDS2Challenge threeDS2Challenge) {
        String url = threeDS2Challenge.getAcsUrl();
        String creq = threeDS2Challenge.getCReq();

        return url + "?creq=" + creq;
    }

    private void checkAccountResultCode(AuthResponse response, PaymentRequestEntity entity, String authNumber) {
        String resultCode = response.getResultCode();
        String status = AUTHORIZED.name();
        String errorCode = StringUtils.EMPTY;
        boolean authorizationOutcome = true;
        if (!resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            status = DENIED.name();
            authorizationOutcome = false;
            errorCode = resultCode;
        }
        entity.setAuthorizationCode(authNumber);
        entity.setAuthorizationOutcome(authorizationOutcome);
        entity.setStatus(status);
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        log.info("END - Vpos Request Payment Account for requestId {} - resultCode: {} ", entity.getGuid(), resultCode);
    }

}
