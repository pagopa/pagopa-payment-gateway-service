package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.dto.ClientConfig;
import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;

@Slf4j
@Service
public class CcPaymentInfoService {
    private static final String AUTHORIZED = "AUTHORIZED";

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private ClientsConfig clientsConfig;

    public CcPaymentInfoResponse getPaymentoInfo(String requestId) {
        Optional<PaymentRequestEntity> paymentInfo = paymentRequestRepository.findByGuidAndRequestEndpoint(requestId, REQUEST_PAYMENTS_VPOS);

        if(!paymentInfo.isPresent()) {
            log.error("No CreditCard request entity has been found for requestId: " + requestId);
            throw new CcHttpException(HttpStatus.NOT_FOUND, "RequestId non trovato");
        }

        return createInfoPaymentResponse(paymentInfo.get());
    }

    private CcPaymentInfoResponse createInfoPaymentResponse(PaymentRequestEntity paymentInfo) {
        CcPaymentInfoResponse response = new CcPaymentInfoResponse();
        response.setStatus(paymentInfo.getStatus());
        response.setRequestId(paymentInfo.getGuid());

        if(AUTHORIZED.equals(paymentInfo.getStatus())) {
            ClientConfig clientConfig = clientsConfig.getByKey(paymentInfo.getClientId());
            response.setClientReturnUrl(clientConfig.getVpos().getClientReturnUrl());
        } else {
            response.setResponseType(paymentInfo.getResponseType());
            response.setVposUrl(paymentInfo.getAuthorizationUrl());
        }

        return response;
    }
}
