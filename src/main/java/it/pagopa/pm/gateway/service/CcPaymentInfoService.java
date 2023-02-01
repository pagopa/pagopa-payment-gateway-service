package it.pagopa.pm.gateway.service;

import com.google.gson.Gson;
import it.pagopa.pm.gateway.dto.ClientConfig;
import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDsMethodData;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.util.Optional;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;

@Slf4j
@Service
public class CcPaymentInfoService {
    private static final String AUTHORIZED = "AUTHORIZED";
    private static final String METHOD = "METHOD";

    @Value("${vpos.method.notifyUrl}")
    String methodNotifyUrl;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private ClientsConfig clientsConfig;

    public CcPaymentInfoResponse getPaymentoInfo(String requestId) {
        Optional<PaymentRequestEntity> paymentInfo = paymentRequestRepository.findByGuidAndRequestEndpoint(requestId, REQUEST_PAYMENTS_VPOS);

        if (!paymentInfo.isPresent()) {
            log.error("No CreditCard request entity has been found for requestId: " + requestId);
            throw new CcHttpException(HttpStatus.NOT_FOUND, "RequestId non trovato");
        }

        return createInfoPaymentResponse(paymentInfo.get());
    }

    private CcPaymentInfoResponse createInfoPaymentResponse(PaymentRequestEntity paymentInfo) {
        CcPaymentInfoResponse response = new CcPaymentInfoResponse();
        String requestId = paymentInfo.getGuid();
        response.setStatus(paymentInfo.getStatus());
        response.setRequestId(requestId);

        if (AUTHORIZED.equals(paymentInfo.getStatus())) {
            ClientConfig clientConfig = clientsConfig.getByKey(paymentInfo.getClientId());
            response.setClientReturnUrl(clientConfig.getVpos().getClientReturnUrl());
        } else {
            if (METHOD.equals(paymentInfo.getResponseType())) {
                String threeDsMethodData = generateBase643DsMethodData(paymentInfo.getIdTransaction(), requestId);
                response.setThreeDsMethodData(threeDsMethodData);
            }
            response.setResponseType(paymentInfo.getResponseType());
            response.setVposUrl(paymentInfo.getAuthorizationUrl());
        }

        return response;
    }

    private String generateBase643DsMethodData(String idTransaction, String requestId) {
        String notifyUrl = String.format(methodNotifyUrl, requestId);

        ThreeDsMethodData threeDsMethodData = new ThreeDsMethodData();
        threeDsMethodData.setTransactionId(idTransaction);
        threeDsMethodData.setNotificationUrl(notifyUrl);

        return Base64Utils.encodeToString(new Gson().toJson(threeDsMethodData).getBytes());
    }
}
