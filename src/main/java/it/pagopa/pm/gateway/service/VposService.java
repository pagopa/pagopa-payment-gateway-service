package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.VposPersistableRequest;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.async.VposAsyncService;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import it.pagopa.pm.gateway.utils.JwtTokenUtils;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_VPOS;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@Service
@Slf4j
@NoArgsConstructor
public class VposService {

    private String vposPollingUrl;
    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private ClientsConfig clientsConfig;
    private JwtTokenUtils jwtTokenUtils;
    private VposAsyncService vposAsyncService;

    @Autowired
    public VposService(PaymentRequestRepository paymentRequestRepository, VPosRequestUtils vPosRequestUtils, ClientsConfig clientsConfig,
                       JwtTokenUtils jwtTokenUtils, @Value("${vpos.polling.url}") String vposPollingUrl, VposAsyncService vposAsyncService) {
        this.vposPollingUrl = vposPollingUrl;
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.clientsConfig = clientsConfig;
        this.jwtTokenUtils = jwtTokenUtils;
        this.vposAsyncService = vposAsyncService;
    }

    public StepZeroResponse startCreditCardPayment(String clientId, String mdcFields, StepZeroRequest request) {
        setMdcFields(mdcFields);
        log.info("START - POST " + REQUEST_PAYMENTS_VPOS);

        if (!clientsConfig.containsKey(clientId)) {
            log.error(String.format("Client id %s is not valid", clientId));
            return createStepZeroResponse(BAD_REQUEST_MSG_CLIENT_ID, null);
        }

        if (ObjectUtils.anyNull(request) || request.getAmount().equals(BigInteger.ZERO)) {
            log.error(BAD_REQUEST_MSG);
            return createStepZeroResponse(BAD_REQUEST_MSG, null);
        }

        String idTransaction = request.getIdTransaction();
        log.info(String.format("START - POST %s for idTransaction %s", REQUEST_PAYMENTS_VPOS, idTransaction));
        if ((Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction)))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createStepZeroResponse(TRANSACTION_ALREADY_PROCESSED_MSG, null);
        }

        try {
            return processStepZero(request, clientId, mdcFields);
        } catch (Exception e) {
            log.error("Error while constructing requestBody for idTransaction {}", idTransaction,e);
            return createStepZeroResponse(GENERIC_ERROR_MSG + request.getIdTransaction(), null);
        }
    }

    private StepZeroResponse processStepZero(StepZeroRequest request, String clientId, String mdcFields) throws Exception {
        PaymentRequestEntity entity = createEntity(clientId, mdcFields, request.getIdTransaction(), request);
        Map<String, String> params = vPosRequestUtils.buildStepZeroRequestParams(request, entity.getGuid());
        vposAsyncService.executeStepZeroAuth(params, entity, request);
        return createStepZeroResponse(null, entity.getGuid());
    }

    private StepZeroResponse createStepZeroResponse(String errorMessage, String requestId) {
        StepZeroResponse response = new StepZeroResponse();

        if (StringUtils.isNotBlank(requestId)) {
            response.setRequestId(requestId);
        }

        if (StringUtils.isEmpty(errorMessage)) {
            String sessionToken = jwtTokenUtils.generateToken(requestId);
            String urlRedirect = vposPollingUrl + requestId + "#token=" + sessionToken;
            response.setUrlRedirect(urlRedirect);
            response.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        } else {
            response.setError(errorMessage);
        }

        log.info(String.format("END - POST %s for requestId %s", REQUEST_PAYMENTS_VPOS, requestId));
        return response;
    }

    private PaymentRequestEntity createEntity(String clientId, String mdcFields, String idTransaction, StepZeroRequest request) throws JsonProcessingException {
        VposPersistableRequest vposPersistableRequest = generateVposDatabaseRequest(request);
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setClientId(clientId);
        entity.setMdcInfo(mdcFields);
        entity.setIdTransaction(idTransaction);
        entity.setGuid(UUID.randomUUID().toString());
        entity.setRequestEndpoint(REQUEST_PAYMENTS_VPOS);
        entity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        entity.setJsonRequest(vposPersistableRequest);
        entity.setIsFirstPayment(request.getIsFirstPayment());
        return entity;
    }

    private VposPersistableRequest generateVposDatabaseRequest(StepZeroRequest request) {
        return new VposPersistableRequest(request.getIdTransaction(),
                request.getAmount(), request.getIsFirstPayment(), request.getIdPsp());
    }

    private boolean checkResultCode(ThreeDS2Response response, PaymentRequestEntity entity) {
        String resultCode = response.getResultCode();
        String status = CREATED.name();
        String responseType = "ERROR";
        String correlationId = StringUtils.EMPTY;
        String responseVposUrl = StringUtils.EMPTY;
        String errorCode = StringUtils.EMPTY;
        String rrn = StringUtils.EMPTY;
        boolean isToAccount = false;
        switch (resultCode) {
            case RESULT_CODE_AUTHORIZED:
                ThreeDS2Authorization authorizedResponse = ((ThreeDS2Authorization) response.getThreeDS2ResponseElement());
                responseType = response.getResponseType().name();
                isToAccount = true;
                correlationId = authorizedResponse.getTransactionId();
                rrn = authorizedResponse.getRrn();
                break;
            case RESULT_CODE_METHOD:
                ThreeDS2Method methodResponse = (ThreeDS2Method) response.getThreeDS2ResponseElement();
                responseType = response.getResponseType().name();
                responseVposUrl = methodResponse.getThreeDSMethodUrl();
                correlationId = methodResponse.getThreeDSTransId();
                break;
            case RESULT_CODE_CHALLENGE:
                ThreeDS2Challenge challengeResponse = ((ThreeDS2Challenge) response.getThreeDS2ResponseElement());
                responseType = response.getResponseType().name();
                responseVposUrl = getChallengeUrl(challengeResponse);
                correlationId = (challengeResponse.getThreeDSTransId());
                break;
            default:
                log.error(String.format("Error resultCode %s from Vpos for requestId %s", resultCode, entity.getGuid()));
                errorCode = resultCode;
                status = DENIED.name();
        }
        entity.setCorrelationId(correlationId);
        entity.setStatus(status);
        entity.setAuthorizationUrl(responseVposUrl);
        entity.setResponseType(responseType);
        entity.setErrorCode(errorCode);
        entity.setRrn(rrn);
        paymentRequestRepository.save(entity);
        return isToAccount;
    }

    private String getChallengeUrl(ThreeDS2Challenge threeDS2Challenge) {
        String url = threeDS2Challenge.getAcsUrl();
        String creq = threeDS2Challenge.getCReq();

        return url + "?creq=" + creq;
    }

    private void checkAccountResultCode(AuthResponse response, PaymentRequestEntity entity, String authNumber) {
        String resultCode = response.getResultCode();
        String status = AUTHORIZED.name();
        String errorCode = StringUtils.EMPTY;
        boolean authorizationOutcome = true;
        if (!resultCode.equals(RESULT_CODE_AUTHORIZED)) {
            status = DENIED.name();
            authorizationOutcome = false;
            errorCode = resultCode;
        }
        entity.setAuthorizationCode(authNumber);
        entity.setAuthorizationOutcome(authorizationOutcome);
        entity.setStatus(status);
        entity.setErrorCode(errorCode);
        paymentRequestRepository.save(entity);
        log.info("END - Vpos Request Payment Account for requestId {} - resultCode: {} ", entity.getGuid(), resultCode);
    }

}
