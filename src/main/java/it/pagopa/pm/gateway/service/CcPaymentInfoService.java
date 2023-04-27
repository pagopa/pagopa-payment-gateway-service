package it.pagopa.pm.gateway.service;

import com.google.gson.Gson;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.enums.VposErrorCodeEnum;
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
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
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
        String vposUrl = getVposUrl(authorizationUrl);
        String creq = getCreqFromAuthUrl(authorizationUrl);
        switch (paymentRequestStatusEnum) {
            case CREATED:
                ThreeDS2ResponseTypeEnum responseTypeEnum = valueOf(paymentRequestEntity.getResponseType());
                response.setThreeDS2ResponseTypeEnum(responseTypeEnum);
                response.setVposUrl(vposUrl);
                fillStepInformation(response, requestId, responseTypeEnum, creq);
                break;
            case AUTHORIZED:
                response.setRedirectUrl(getRedirectUrl(paymentRequestEntity));
                response.setCreq(creq);
                response.setOutcomeVposGateway(buildOutcomeVposGateway(paymentRequestEntity, OK));
                break;
            case CANCELLED:
            case DENIED:
            default:
                OutcomeEnum outcomeEnum = paymentRequestStatusEnum.equals(CANCELLED) ? OK : KO;
                response.setOutcomeVposGateway(buildOutcomeVposGateway(paymentRequestEntity, outcomeEnum));
                response.setRedirectUrl(getRedirectUrl(paymentRequestEntity));
                break;
        }
        return response;
    }

    private String getVposUrl(String authorizationUrl) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(authorizationUrl);
        List<NameValuePair> queryParams = uriBuilder.getQueryParams();
        queryParams.removeIf(queryParameter -> queryParameter.getName().equals(CREQ));
        uriBuilder.setParameters(queryParams);
        return uriBuilder.build().toString();
    }

    private String getCreqFromAuthUrl(String authorizationUrl) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(authorizationUrl);
        NameValuePair creqPair = uriBuilder.getQueryParams().stream()
                .filter(queryParam -> StringUtils.equals(queryParam.getName(), CREQ))
                .findFirst()
                .orElse(null);
        return Objects.nonNull(creqPair) ? creqPair.getValue() : null;
    }

    private OutcomeVposGateway buildOutcomeVposGateway(PaymentRequestEntity paymentRequestEntity, OutcomeEnum outcomeEnum) {
        OutcomeVposGateway outcomeVposGateway = new OutcomeVposGateway();
        outcomeVposGateway.setOutcomeEnum(outcomeEnum);
        outcomeVposGateway.setRrn(paymentRequestEntity.getRrn());
        outcomeVposGateway.setAuthorizationCode(paymentRequestEntity.getAuthorizationCode());
        String errorCode = paymentRequestEntity.getErrorCode();
        VposErrorCodeEnum errorCodeEnum = StringUtils.isNotBlank(errorCode) ? VposErrorCodeEnum.valueOf(errorCode) : null;
        outcomeVposGateway.setVposErrorCodeEnum(errorCodeEnum);
        return outcomeVposGateway;
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
