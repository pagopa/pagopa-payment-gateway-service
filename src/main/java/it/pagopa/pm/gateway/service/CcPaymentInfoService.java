package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;

@Slf4j
@Service
public class CcPaymentInfoService {
    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

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
        response.setResponseType(paymentInfo.getResponseType());
        response.setRequestId(paymentInfo.getGuid());
        response.setVposUrl(paymentInfo.getAuthorizationUrl());

        return response;
    }
}
