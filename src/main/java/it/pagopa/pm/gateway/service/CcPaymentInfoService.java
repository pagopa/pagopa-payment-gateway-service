package it.pagopa.pm.gateway.service;

import com.google.gson.Gson;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.vpos.CcHttpException;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.dto.vpos.OutcomeVposGatewayResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDsMethodData;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pm.gateway.constant.ApiPaths.VPOS_AUTHORIZATIONS;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.KO;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.OK;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CANCELLED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.getEnumValueFromString;
import static it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum.*;

@Slf4j
@Service
@NoArgsConstructor
public class CcPaymentInfoService {

    private static final String CREQ = "creq";
    private String methodNotifyUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private ClientsConfig clientsConfig;

    @Autowired
    public CcPaymentInfoService(@Value("${vpos.method.notifyUrl}") String methodNotifyUrl, PaymentRequestRepository paymentRequestRepository, ClientsConfig clientsConfig) {
        this.methodNotifyUrl = methodNotifyUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.clientsConfig = clientsConfig;
    }

    public CcPaymentInfoResponse getPaymentInfo(String requestId) {
        Optional<PaymentRequestEntity> paymentInfo = paymentRequestRepository.findByGuidAndRequestEndpoint(requestId, VPOS_AUTHORIZATIONS);

        if (!paymentInfo.isPresent()) {
            log.error("No CreditCard request entity has been found for requestId: " + requestId);
            throw new CcHttpException(HttpStatus.NOT_FOUND, "RequestId non trovato");
        }

        try {
            return buildCcPaymentInfoResponse(paymentInfo.get());
        } catch (URISyntaxException e) {
            log.error("Error while extracting VPosUrl or creq from authorizationUrl", e);
            throw new CcHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore durante la decodifica dell'authorizationUrl");
        }
    }

    private CcPaymentInfoResponse buildCcPaymentInfoResponse(PaymentRequestEntity paymentRequestEntity) throws URISyntaxException {
        CcPaymentInfoResponse response = new CcPaymentInfoResponse();
        String requestId = paymentRequestEntity.getGuid();
        PaymentRequestStatusEnum paymentRequestStatusEnum =
                getEnumValueFromString(paymentRequestEntity.getStatus());
        response.setPaymentRequestStatusEnum(paymentRequestStatusEnum);
        response.setRequestId(requestId);
        String authorizationUrl = paymentRequestEntity.getAuthorizationUrl();
        String vposUrl = null;
        String creq = null;
        if (StringUtils.isNotBlank(authorizationUrl)) {
            vposUrl = getVposUrl(authorizationUrl);
            creq = getCreqFromAuthUrl(authorizationUrl);
        }
        switch (paymentRequestStatusEnum) {
            case CREATED:
                ThreeDS2ResponseTypeEnum responseTypeEnum = valueOf(paymentRequestEntity.getResponseType());
                response.setThreeDS2ResponseTypeEnum(responseTypeEnum);
                response.setVposUrl(vposUrl);
                fillStepInformation(response, requestId, responseTypeEnum, creq);
                break;
            case AUTHORIZED:
                response.setRedirectUrl(getRedirectUrl(paymentRequestEntity));
                response.setOutcomeVposGatewayResponse(buildOutcomeVposGateway(paymentRequestEntity, OK));
                break;
            case CANCELLED:
            case DENIED:
            default:
                OutcomeEnum outcomeEnum = paymentRequestStatusEnum.equals(CANCELLED) ? OK : KO;
                response.setOutcomeVposGatewayResponse(buildOutcomeVposGateway(paymentRequestEntity, outcomeEnum));
                response.setRedirectUrl(getRedirectUrl(paymentRequestEntity));
                break;
        }
        return response;
    }

    private String getVposUrl(String authorizationUrl) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(authorizationUrl);
        List<NameValuePair> queryParams = uriBuilder.getQueryParams();
        queryParams.removeIf(queryParam -> StringUtils.equals(queryParam.getName(), CREQ));
        uriBuilder.setParameters(queryParams);
        return uriBuilder.build().toString();
    }

    private String getCreqFromAuthUrl(String authorizationUrl) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(authorizationUrl);
        return uriBuilder.getQueryParams().stream()
                .filter(queryParam -> StringUtils.equals(queryParam.getName(), CREQ))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }

    private OutcomeVposGatewayResponse buildOutcomeVposGateway(PaymentRequestEntity paymentRequestEntity, OutcomeEnum outcomeEnum) {
        OutcomeVposGatewayResponse outcomeVposGatewayResponse = new OutcomeVposGatewayResponse();
        outcomeVposGatewayResponse.setOutcomeEnum(outcomeEnum);
        outcomeVposGatewayResponse.setRrn(paymentRequestEntity.getRrn());
        outcomeVposGatewayResponse.setAuthorizationCode(paymentRequestEntity.getAuthorizationCode());
        outcomeVposGatewayResponse.setErrorCode(paymentRequestEntity.getErrorCode());
        return outcomeVposGatewayResponse;
    }

    private void fillStepInformation(CcPaymentInfoResponse response, String requestId,
                                     ThreeDS2ResponseTypeEnum responseTypeEnum, String creq) {
        if (responseTypeEnum.equals(METHOD)) {
            response.setThreeDsMethodData(generate3DsMethodData(requestId));
        } else if (responseTypeEnum.equals(CHALLENGE)) {
            response.setCreq(creq);
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
}
