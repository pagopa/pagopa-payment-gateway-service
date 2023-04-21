package it.pagopa.pm.gateway.service;

import com.google.gson.Gson;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.dto.vpos.OutcomeVposGateway;
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
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.*;
import static it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum.*;
import static it.pagopa.pm.gateway.dto.enums.VposErrorCodeEnum.*;

@Slf4j
@Service
@NoArgsConstructor
public class CcPaymentInfoService {

    public static final String CREQ = "?creq=";
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

        return buildCcPaymentInfoResponse(paymentInfo.get());
    }

    private CcPaymentInfoResponse buildCcPaymentInfoResponse(PaymentRequestEntity paymentRequestEntity) {
        CcPaymentInfoResponse response = new CcPaymentInfoResponse();
        String requestId = paymentRequestEntity.getGuid();
        PaymentRequestStatusEnum paymentRequestStatusEnum =
                PaymentRequestStatusEnum.getEnumValueFromString(paymentRequestEntity.getStatus());
        response.setPaymentRequestStatusEnum(paymentRequestStatusEnum);
        response.setRequestId(requestId);

        switch (paymentRequestStatusEnum) {
            case CREATED:
                ThreeDS2ResponseTypeEnum responseTypeEnum = getEnumFromValue(paymentRequestEntity.getResponseType());
                response.setThreeDS2ResponseTypeEnum(responseTypeEnum);
                response.setVposUrl(paymentRequestEntity.getAuthorizationUrl());
                fillStepInformation(paymentRequestEntity, response, requestId, responseTypeEnum);
                break;
            case AUTHORIZED:
                response.setRedirectUrl(getRedirectUrl(paymentRequestEntity));
                response.setCreq(getCreqFromChallengeUrl(paymentRequestEntity.getAuthorizationUrl()));
                response.setOutcomeVposGateway(buildOutcomeVposGateway(paymentRequestEntity, OK));
                break;
            default: // DENIED/CANCELLED
                response.setOutcomeVposGateway(buildOutcomeVposGateway(paymentRequestEntity, KO));
        }
        return response;
    }

    private OutcomeVposGateway buildOutcomeVposGateway(PaymentRequestEntity paymentRequestEntity, OutcomeEnum outcomeEnum) {
        OutcomeVposGateway outcomeVposGateway = new OutcomeVposGateway();
        outcomeVposGateway.setOutcomeEnum(outcomeEnum);
        outcomeVposGateway.setRrn(paymentRequestEntity.getRrn());
        outcomeVposGateway.setAuthorizationCode(paymentRequestEntity.getAuthorizationCode());
        outcomeVposGateway.setVposErrorCodeEnum(getEnumFromCode(paymentRequestEntity.getErrorCode()));
        return outcomeVposGateway;
    }

    private void fillStepInformation(PaymentRequestEntity paymentRequestEntity, CcPaymentInfoResponse response,
                                     String requestId, ThreeDS2ResponseTypeEnum responseTypeEnum) {
        switch (responseTypeEnum) {
            case METHOD:
                response.setThreeDsMethodData(generate3DsMethodData(requestId));
                break;
            case CHALLENGE:
                response.setCreq(getCreqFromChallengeUrl(paymentRequestEntity.getAuthorizationUrl()));
                break;
            default: // AUTHORIZATION/ERROR
                response.setRedirectUrl(getRedirectUrl(paymentRequestEntity));
                break;
        }
    }

    private String getRedirectUrl(PaymentRequestEntity paymentRequestEntity) {
        ClientConfig clientConfig = clientsConfig.getByKey(paymentRequestEntity.getClientId());
        String clientReturnUrl = clientConfig.getVpos().getClientReturnUrl();
        return StringUtils.join(clientReturnUrl, paymentRequestEntity.getIdTransaction());
    }

    private String generate3DsMethodData(String requestId) {
        String notifyUrl = String.format(methodNotifyUrl, requestId);

        ThreeDsMethodData threeDsMethodData = new ThreeDsMethodData();
        threeDsMethodData.setThreeDSServerTransID(requestId);
        threeDsMethodData.setThreeDSMethodNotificationURL(notifyUrl);

        return Base64Utils.encodeToString(new Gson().toJson(threeDsMethodData).getBytes());
    }

    private String getCreqFromChallengeUrl(String challengeUrl) {
        int creqStart = challengeUrl.lastIndexOf(CREQ);
        return challengeUrl.substring(creqStart + CREQ.length());
    }
}
