package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.RefundOutcome;
import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.transaction.UpdateAuthRequest;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.VposOrderStatusResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static it.pagopa.pm.gateway.constant.Messages.GENERIC_REFUND_ERROR_MSG;
import static it.pagopa.pm.gateway.constant.Messages.PATCH_CLOSE_PAYMENT_ERROR;
import static it.pagopa.pm.gateway.constant.VposConstant.RESULT_CODE_AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CANCELLED;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.KO;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.OK;

@Slf4j
@NoArgsConstructor
@Component
public class VposPatchUtils {

    private String vposUrl;

    private PaymentRequestRepository paymentRequestRepository;

    private EcommerceClient ecommerceClient;

    private VPosRequestUtils vPosRequestUtils;

    private VPosResponseUtils vPosResponseUtils;

    private ClientsConfig clientsConfig;

    private HttpClient httpClient;


    @Autowired
    public VposPatchUtils(@Value("${vpos.requestUrl}") String vposUrl, PaymentRequestRepository paymentRequestRepository,
                          EcommerceClient ecommerceClient, VPosRequestUtils vPosRequestUtils, VPosResponseUtils vPosResponseUtils,
                          ClientsConfig clientsConfig, HttpClient httpClient) {
        this.vposUrl = vposUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.ecommerceClient = ecommerceClient;
        this.vPosRequestUtils = vPosRequestUtils;
        this.vPosResponseUtils = vPosResponseUtils;
        this.clientsConfig = clientsConfig;
        this.httpClient = httpClient;
    }

    public void executePatchTransaction(PaymentRequestEntity entity, StepZeroRequest pgsRequest) throws IOException {
        String requestId = entity.getGuid();
        log.info("START - PATCH updateTransaction for requestId: {}", requestId);
        AuthResultEnum authResult = entity.getStatus().equals(AUTHORIZED.name()) ? AuthResultEnum.OK : AuthResultEnum.KO;

        String authCode;
        if (AUTHORIZED.name().equals(entity.getStatus())) {
            authCode = entity.getAuthorizationCode();
        } else {
            authCode = entity.getErrorCode();
        }

        UpdateAuthRequest patchRequest = new UpdateAuthRequest(authResult, authCode);
        try {
            ClientConfig clientConfig = clientsConfig.getByKey(entity.getClientId());
            TransactionInfo patchResponse = ecommerceClient.callPatchTransaction(patchRequest, entity.getIdTransaction(), clientConfig);
            log.info("Response from PATCH updateTransaction for requestId {} is {}", requestId, patchResponse.toString());
        } catch (Exception e) {
            log.error("{}{}", PATCH_CLOSE_PAYMENT_ERROR, requestId, e);
            log.info("Refunding payment with requestId: {}", requestId);
            if (executeOrderStatus(entity, pgsRequest).equals(OK)) {
                executeRevert(entity, pgsRequest);
            }
        }
    }

    private RefundOutcome executeOrderStatus(PaymentRequestEntity entity, StepZeroRequest stepZeroRequest) throws IOException {
        log.info("Calling VPOS - OrderStatus - for requestId: {}", entity.getGuid());
        Map<String, String> params = vPosRequestUtils.buildOrderStatusParams(stepZeroRequest);
        HttpClientResponse clientResponse = callVPos(params);
        VposOrderStatusResponse response = vPosResponseUtils.buildOrderStatusResponse(clientResponse.getEntity());
        return computeOrderStatusResultCode(response, entity);
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

    private void executeRevert(PaymentRequestEntity entity, StepZeroRequest pgsRequest) {
        try {
            log.info("Calling VPOS - Revert - for requestId: {}", entity.getGuid());
            Map<String, String> params = vPosRequestUtils.buildRevertRequestParams(pgsRequest, entity.getCorrelationId());
            HttpClientResponse clientResponse = callVPos(params);
            AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse.getEntity());
            vPosResponseUtils.validateResponseMac(response.getTimestamp(), response.getResultCode(), response.getResultMac(), pgsRequest);
            checkRevertResultCode(response, entity);
        } catch (Exception e) {
            log.error("{}{} cause: {} - {}", GENERIC_REFUND_ERROR_MSG, entity.getIdTransaction(), e.getCause(), e.getMessage(), e);
        }
    }

    private void checkRevertResultCode(AuthResponse response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        if (resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            entity.setStatus(CANCELLED.name());
            entity.setIsRefunded(true);
            paymentRequestRepository.save(entity);
        } else {
            entity.setErrorMessage("Error during Revert");
            entity.setIsRefunded(false);
        }
        log.info("END - VPos Request Payment Revert for requestId {}  - resultCode: {}" + entity.getGuid(), resultCode);
    }

    private HttpClientResponse callVPos(Map<String, String> params) throws IOException {
        HttpClientResponse clientResponse = httpClient.post(vposUrl, ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), params);
        if (clientResponse.getStatus() != HttpStatus.OK.value()) {
            log.error("HTTP Response Status: " + clientResponse.getStatus());
            throw new IOException("Non-ok response from VPos. HTTP status: " + clientResponse.getStatus());
        }
        return clientResponse;
    }
}
