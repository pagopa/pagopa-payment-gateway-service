package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.RefundOutcome;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.VposDeleteResponse;
import it.pagopa.pm.gateway.dto.vpos.VposOrderStatusResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CANCELLED;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.KO;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.OK;

@Service
@Slf4j
public class VposDeleteService {

    @Value("${vpos.requestUrl}")
    private String vposUrl;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private VPosRequestUtils vPosRequestUtils;

    @Autowired
    private VPosResponseUtils vPosResponseUtils;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    public VposDeleteResponse startDelete(String requestId) {
        log.info("START - Vpos refund for requestId: " + requestId);
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error(REQUEST_ID_NOT_FOUND_MSG);
            return createDeleteResponse(requestId, REQUEST_ID_NOT_FOUND_MSG, KO, null);
        }

        if (BooleanUtils.isTrue(entity.getIsRefunded())) {
            log.info("RequestId " + requestId + " has been refunded already. Skipping refund");
            return createDeleteResponse(requestId, String.format(TRANSACTION_ALREADY_REFUND, entity.getIdTransaction()), KO, entity);
        }

        RefundOutcome refundOutcome;
        try {
            StepZeroRequest stepZeroRequest = getStepZeroRequest(entity);
            refundOutcome = executeOrderStatus(entity, stepZeroRequest);
            if (refundOutcome.equals(OK)) {
                refundOutcome = executeRevert(entity, stepZeroRequest);
                if(!refundOutcome.equals(OK)) {
                    return createDeleteResponse(requestId, entity.getErrorMessage(), refundOutcome, entity);
                }
            } else {
                return createDeleteResponse(requestId, entity.getErrorMessage(), refundOutcome, entity);
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + e);
            return createDeleteResponse(requestId, GENERIC_REFUND_ERROR_MSG + requestId, KO, entity);
        }
        return createDeleteResponse(requestId, null, refundOutcome, entity);
    }

    private StepZeroRequest getStepZeroRequest(PaymentRequestEntity entity) throws JsonProcessingException {
        StepZeroRequest stepZeroRequest = objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class);
        stepZeroRequest.setIsFirstPayment(false);
        return stepZeroRequest;
    }

    private RefundOutcome executeOrderStatus(PaymentRequestEntity entity, StepZeroRequest stepZeroRequest) throws IOException {
        log.info("Calling VPOS - OrderStatus - for requestId: " + entity.getGuid());
        Map<String, String> params = vPosRequestUtils.buildOrderStatusParams(stepZeroRequest, entity.getCorrelationId());
        HttpClientResponse clientResponse = callVPos(params);
        VposOrderStatusResponse response = vPosResponseUtils.buildOrderStatusResponse(clientResponse.getEntity());
        return checkOrderStatusResultCode(response, entity);
    }

    private RefundOutcome executeRevert(PaymentRequestEntity entity, StepZeroRequest stepZeroRequest) throws IOException {
        log.info("Calling VPOS - Revert - for requestId: " + entity.getGuid());
        Map<String, String> params = vPosRequestUtils.buildRevertRequestParams(stepZeroRequest, entity.getCorrelationId());
        HttpClientResponse clientResponse = callVPos(params);
        AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
        return checkRevertResultCode(response, entity);
    }

    private VposDeleteResponse createDeleteResponse(String requestId, String errorMessage, RefundOutcome refundOutcome, PaymentRequestEntity entity) {
        VposDeleteResponse response = new VposDeleteResponse();
        response.setRequestId(requestId);
        response.setError(errorMessage);
        response.setRefundOutcome(refundOutcome.name());
        if(Objects.nonNull(entity)) {
            response.setStatus(entity.getStatus());
        }

        log.info("END - Vpos refund for requestId: " + requestId);
        return response;
    }

    private HttpClientResponse callVPos(Map<String, String> params) throws IOException {
        HttpClientResponse clientResponse = httpClient.post(vposUrl, ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), params);
        if (clientResponse.getStatus() != HttpStatus.OK.value()) {
            log.error("HTTP Response Status: " + clientResponse.getStatus());
            throw new IOException("Non-ok response from VPos. HTTP status: " + clientResponse.getStatus());
        }
        return clientResponse;
    }

    private RefundOutcome computeOrderStatusResultCode(VposOrderStatusResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        if (resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            return OK;
        } else {
            entity.setErrorMessage("Error during orderStatus");
            paymentRequestRepository.save(entity);
            return KO;
        }
    }

    private RefundOutcome checkRevertResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        RefundOutcome refundOutcome;
        if (resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            entity.setStatus(CANCELLED.name());
            entity.setIsRefunded(true);
            refundOutcome = OK;
        } else {
            entity.setErrorMessage("Error during Revert");
            refundOutcome = KO;
        }
        paymentRequestRepository.save(entity);
        return refundOutcome;
    }

}
