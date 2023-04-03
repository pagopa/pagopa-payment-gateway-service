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
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
public class VposDeleteService {

    private String vposUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private VPosResponseUtils vPosResponseUtils;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @Autowired
    public VposDeleteService(PaymentRequestRepository paymentRequestRepository,
                             VPosRequestUtils vPosRequestUtils, VPosResponseUtils vPosResponseUtils,
                             HttpClient httpClient, ObjectMapper objectMapper, @Value("${vpos.requestUrl}") String vposUrl) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.vPosResponseUtils = vPosResponseUtils;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.vposUrl = vposUrl;
    }

    public VposDeleteResponse startDelete(String requestId) {
        log.info("START - Vpos refund for requestId: " + requestId);
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error(REQUEST_ID_NOT_FOUND_MSG);
            return createDeleteResponse(requestId, REQUEST_ID_NOT_FOUND_MSG, null);
        }

        if (BooleanUtils.isTrue(entity.getIsRefunded())) {
            log.info("RequestId " + requestId + " has been refunded already. Skipping refund");
            return createDeleteResponse(requestId, String.format(TRANSACTION_ALREADY_REFUND, entity.getIdTransaction()), entity);
        }

        RefundOutcome refundOutcome;
        try {
            StepZeroRequest stepZeroRequest = getStepZeroRequest(entity);
            refundOutcome = executeOrderStatus(entity, stepZeroRequest);
            if (refundOutcome.equals(OK)) {
                refundOutcome = executeRevert(entity, stepZeroRequest);
                if (!refundOutcome.equals(OK)) {
                    return createDeleteResponse(requestId, entity.getErrorMessage(), entity);
                } else {
                    return createDeleteResponse(requestId, null, entity);
                }
            } else {
                return createDeleteResponse(requestId, entity.getErrorMessage(), entity);
            }
        } catch (Exception e) {
            log.error(GENERIC_ERROR_MSG + entity.getIdTransaction() + e);
            return createDeleteResponse(requestId, GENERIC_REFUND_ERROR_MSG + requestId, entity);
        }
    }

    private StepZeroRequest getStepZeroRequest(PaymentRequestEntity entity) throws JsonProcessingException {
        StepZeroRequest stepZeroRequest = objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class);
        stepZeroRequest.setIsFirstPayment(false);
        return stepZeroRequest;
    }

    private RefundOutcome executeOrderStatus(PaymentRequestEntity entity, StepZeroRequest stepZeroRequest) throws IOException {
        log.info("Calling VPOS - OrderStatus - for requestId: " + entity.getGuid());
        Map<String, String> params = vPosRequestUtils.buildOrderStatusParams(stepZeroRequest);
        HttpClientResponse clientResponse = callVPos(params);
        VposOrderStatusResponse response = vPosResponseUtils.buildOrderStatusResponse(clientResponse.getEntity());
        return computeOrderStatusResultCode(response, entity);
    }

    private RefundOutcome executeRevert(PaymentRequestEntity entity, StepZeroRequest stepZeroRequest) throws IOException {
        log.info("Calling VPOS - Revert - for requestId: " + entity.getGuid());
        Map<String, String> params = vPosRequestUtils.buildRevertRequestParams(stepZeroRequest, entity.getCorrelationId());
        HttpClientResponse clientResponse = callVPos(params);
        AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
        return saveRevertResultCode(response, entity);
    }

    private VposDeleteResponse createDeleteResponse(String requestId, String errorMessage, PaymentRequestEntity entity) {
        VposDeleteResponse response = new VposDeleteResponse();
        response.setRequestId(requestId);
        response.setError(errorMessage);
        if (Objects.nonNull(entity)) {
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

    private RefundOutcome saveRevertResultCode(AuthResponse response, PaymentRequestEntity entity) {
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
