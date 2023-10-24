package it.pagopa.pm.gateway.service.async;

import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Authorization;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
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
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;

@Service
@Slf4j
@NoArgsConstructor
public class CcResumeStep2AsyncService {

    private String vposUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private VPosResponseUtils vPosResponseUtils;
    private HttpClient httpClient;
    private EcommercePatchUtils ecommercePatchUtils;

    @Autowired
    public CcResumeStep2AsyncService(@Value("${vpos.requestUrl}") String vposUrl, PaymentRequestRepository paymentRequestRepository,
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
    public void executeStep2(Map<String, String> params, PaymentRequestEntity entity, StepZeroRequest request) {
        try {
            MdcUtils.setMdcFields(entity.getMdcInfo());
            String requestId = entity.getGuid();
            log.info("Calling VPOS - Step 2 - for requestId: " + requestId);
            HttpClientResponse clientResponse = httpClient.callVPos(vposUrl, params);
            ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), request);
            log.info("Result code from VPOS - Step 2 - for RequestId {} is {}", requestId, response.getResultCode());
            if (isStepTwoResultCodeOk(response, entity)) {
                executeAccount(entity, request);
            }

            ecommercePatchUtils.executePatchTransactionVPos(entity);
        } catch (Exception e) {
            log.error("{}{}", GENERIC_ERROR_MSG, entity.getIdTransaction(), e);
            if (PaymentRequestStatusEnum.PROCESSING.name().equals(entity.getStatus())) {
                entity.setStatus(PaymentRequestStatusEnum.CREATED.name());
                paymentRequestRepository.save(entity);
            }
        }
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
