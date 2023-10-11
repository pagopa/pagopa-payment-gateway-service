package it.pagopa.pm.gateway.service.async;

import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.vpos.*;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.EcommercePatchUtils;
import it.pagopa.pm.gateway.utils.MdcUtils;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

import static it.pagopa.pm.gateway.constant.Messages.GENERIC_ERROR_MSG;
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_AUTHORIZED;
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_CHALLENGE;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;

@Service
@Slf4j
@NoArgsConstructor
public class CcResumeStep1AsyncService {

    public static final String RESULT_CODE_METHOD = "26";

    private String vposUrl;

    private PaymentRequestRepository paymentRequestRepository;

    private VPosRequestUtils vPosRequestUtils;

    private VPosResponseUtils vPosResponseUtils;

    private HttpClient httpClient;

    private EcommercePatchUtils ecommercePatchUtils;

    @Autowired
    public CcResumeStep1AsyncService(@Value("${vpos.requestUrl}") String vposUrl, PaymentRequestRepository paymentRequestRepository,
                                     VPosRequestUtils vPosRequestUtils, VPosResponseUtils vPosResponseUtils,
                                     HttpClient httpClient, EcommercePatchUtils ecommercePatchUtils) {
        this.vposUrl = vposUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.vPosResponseUtils = vPosResponseUtils;
        this.httpClient = httpClient;
        this.ecommercePatchUtils = ecommercePatchUtils;
    }

    //@Async
    public void executeStep1(Map<String, String> params, PaymentRequestEntity entity, StepZeroRequest request) {
        try {
            MdcUtils.setMdcFields(entity.getMdcInfo());
            String requestId = entity.getGuid();
            log.info("Calling VPOS - Step 1 - for requestId: " + requestId);
            HttpClientResponse clientResponse = httpClient.callVPos(vposUrl, params);
            ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), request);
            log.info("Result code from VPOS - Step 1 - for RequestId {} is {}", requestId, response.getResultCode());
            if (isStepOneResultCodeOk(response, entity)) {
                executeAccount(entity, request);
            }

            //If the resultCode is 26, the PATCH is not called
            if (!RESULT_CODE_METHOD.equals(response.getResultCode())) {
                ecommercePatchUtils.executePatchTransactionVPos(entity);
            }
        } catch (Exception e) {
            log.error("{}{}", GENERIC_ERROR_MSG, entity.getIdTransaction(), e);
            if (PaymentRequestStatusEnum.PROCESSING.name().equals(entity.getStatus())) {
                entity.setStatus(PaymentRequestStatusEnum.CREATED.name());
                paymentRequestRepository.save(entity);
            }
        }
    }

    private boolean isStepOneResultCodeOk(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = entity.getResponseType();
        String responseVposUrl = StringUtils.EMPTY;
        String correlationId = entity.getCorrelationId();
        String errorCode = StringUtils.EMPTY;
        String rrn = entity.getRrn();
        boolean isToAccount = false;
        ThreeDS2ResponseElement threeDS2ResponseElement = response.getThreeDS2ResponseElement();
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                ThreeDS2Authorization authorizedResponse = ((ThreeDS2Authorization) threeDS2ResponseElement);
                responseType = response.getResponseType().name();
                isToAccount = true;
                correlationId = authorizedResponse.getTransactionId();
                rrn = authorizedResponse.getRrn();
                break;
            case RESULT_CODE_CHALLENGE:
                ThreeDS2Challenge challengeResponse = (ThreeDS2Challenge) threeDS2ResponseElement;
                responseType = response.getResponseType().name();
                responseVposUrl = vPosResponseUtils.getChallengeUrl(challengeResponse);
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
        entity.setRrn(rrn);
        paymentRequestRepository.save(entity);
        return isToAccount;
    }

    private void executeAccount(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Accounting - for requestId: {}", entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildAccountingRequestParams(pgsRequest, entity.getCorrelationId());
            HttpClientResponse clientResponse = httpClient.callVPos(vposUrl, params);
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
        log.info("END - Vpos Request Payment Account for requestId {} - resultCode: {}", entity.getGuid(), resultCode);
    }
}
