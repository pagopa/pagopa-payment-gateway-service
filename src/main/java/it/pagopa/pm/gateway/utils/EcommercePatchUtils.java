package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.enums.PaymentGatewayTypeEnum;
import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.vpos.UpdateAuthRequestVPos;
import it.pagopa.pm.gateway.dto.xpay.UpdateAuthRequestXPay;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static it.pagopa.pm.gateway.constant.Messages.PATCH_CLOSE_PAYMENT_ERROR;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.AUTHORIZED;

@Slf4j
@NoArgsConstructor
@Component
public class EcommercePatchUtils {

    private EcommerceClient ecommerceClient;

    private ClientsConfig clientsConfig;

    @Autowired
    public EcommercePatchUtils(EcommerceClient ecommerceClient, ClientsConfig clientsConfig) {
        this.ecommerceClient = ecommerceClient;
        this.clientsConfig = clientsConfig;
    }

    public void executePatchTransactionXPay(PaymentRequestEntity entity) {
        String requestId = entity.getGuid();
        log.info("START - PATCH updateTransaction for requestId: {}", requestId);
        AuthResultEnum authResult = entity.getStatus().equals(AUTHORIZED.name()) ? AuthResultEnum.OK : AuthResultEnum.KO;

        UpdateAuthRequestXPay patchRequest = new UpdateAuthRequestXPay(PaymentGatewayTypeEnum.XPAY.name(), authResult, entity.getAuthorizationCode(), entity.getErrorCode());
        try {
            ClientConfig clientConfig = clientsConfig.getByKey(entity.getClientId());
            TransactionInfo patchResponse = ecommerceClient.callPatchTransaction(patchRequest, entity.getIdTransaction(), clientConfig);
            log.info("Response from PATCH updateTransaction for requestId {} is {}", requestId, patchResponse.toString());
        } catch (Exception e) {
            log.error("{}{}", PATCH_CLOSE_PAYMENT_ERROR, requestId, e);
        }
    }

    public void executePatchTransactionVPos(PaymentRequestEntity entity) {
        String requestId = entity.getGuid();
        log.info("START - PATCH updateTransaction for requestId: {}", requestId);
        AuthResultEnum authResult = entity.getStatus().equals(AUTHORIZED.name()) ? AuthResultEnum.OK : AuthResultEnum.KO;

        UpdateAuthRequestVPos patchRequest = new UpdateAuthRequestVPos(PaymentGatewayTypeEnum.VPOS.name(), authResult, entity.getRrn(), entity.getAuthorizationCode(), entity.getErrorCode());
        try {
            ClientConfig clientConfig = clientsConfig.getByKey(entity.getClientId());
            TransactionInfo patchResponse = ecommerceClient.callPatchTransaction(patchRequest, entity.getIdTransaction(), clientConfig);
            log.info("Response from PATCH updateTransaction for requestId {} is {}", requestId, patchResponse.toString());
        } catch (Exception e) {
            log.error("{}{}", PATCH_CLOSE_PAYMENT_ERROR, requestId, e);
        }
    }
}
