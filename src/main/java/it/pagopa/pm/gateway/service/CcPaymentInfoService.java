package it.pagopa.pm.gateway.service;

import com.google.gson.Gson;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDsMethodData;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.util.Optional;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;

@Slf4j
@Service
@NoArgsConstructor
public class CcPaymentInfoService {

    public static final String CREQ = "?creq=";
    private static final String STATUS_AUTHORIZED = "AUTHORIZED";
    private static final String RESP_TYPE_METHOD = "METHOD";
    private static final String RESP_TYPE_CHALLENGE = "CHALLENGE";
    private static final String STATUS_CREATED = "CREATED";
    private String methodNotifyUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private ClientsConfig clientsConfig;

    @Autowired
    public CcPaymentInfoService(@Value("${vpos.method.notifyUrl}") String methodNotifyUrl, PaymentRequestRepository paymentRequestRepository, ClientsConfig clientsConfig) {
        this.methodNotifyUrl = methodNotifyUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.clientsConfig = clientsConfig;
    }

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
        String idTransaction = paymentInfo.getIdTransaction();
        response.setStatus(paymentInfo.getStatus());
        response.setRequestId(requestId);

        if (STATUS_AUTHORIZED.equals(paymentInfo.getStatus())) {
            response.setAuthCode(paymentInfo.getAuthorizationCode());
        } else if(STATUS_CREATED.equals(paymentInfo.getStatus())) {
            if (RESP_TYPE_METHOD.equals(paymentInfo.getResponseType())) {
                String threeDsMethodData = generate3DsMethodData(requestId);
                response.setThreeDsMethodData(threeDsMethodData);
                response.setResponseType(paymentInfo.getResponseType());
            } else if (RESP_TYPE_CHALLENGE.equals(paymentInfo.getResponseType())) {
                String creq = getCreqFromChallengeUrl(paymentInfo);
                response.setCreq(creq);
                response.setResponseType(paymentInfo.getResponseType());
            }
            response.setVposUrl(paymentInfo.getAuthorizationUrl());
        }

        if(!STATUS_CREATED.equals(paymentInfo.getStatus())) {
            ClientConfig clientConfig = clientsConfig.getByKey(paymentInfo.getClientId());
            String clientReturnUrl = clientConfig.getXpay().getClientReturnUrl();
            response.setRedirectUrl(StringUtils.join(clientReturnUrl, idTransaction));
        }

        return response;
    }

    private String generate3DsMethodData(String requestId) {
        String notifyUrl = String.format(methodNotifyUrl, requestId);

        ThreeDsMethodData threeDsMethodData = new ThreeDsMethodData();
        threeDsMethodData.setThreeDSServerTransID(requestId);
        threeDsMethodData.setThreeDSMethodNotificationURL(notifyUrl);

        return Base64Utils.encodeToString(new Gson().toJson(threeDsMethodData).getBytes());
    }

    private String getCreqFromChallengeUrl(PaymentRequestEntity entity) {
        String challengeUrl = entity.getAuthorizationUrl();
        int index = challengeUrl.lastIndexOf(CREQ);
        String url = challengeUrl.substring(0, index);
        entity.setAuthorizationUrl(url);
        return challengeUrl.substring(index + CREQ.length());
    }
}
