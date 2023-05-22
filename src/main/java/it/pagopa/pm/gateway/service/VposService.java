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

import static it.pagopa.pm.gateway.constant.ApiPaths.VPOS_AUTHORIZATIONS;
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
        log.info("START - POST {}", VPOS_AUTHORIZATIONS);

        if (!clientsConfig.containsKey(clientId)) {
            log.error(String.format("Client id %s is not valid", clientId));
            return createStepZeroResponse(BAD_REQUEST_MSG_CLIENT_ID, null);
        }

        if (ObjectUtils.anyNull(request) || request.getAmount().equals(BigInteger.ZERO)) {
            log.error(BAD_REQUEST_MSG);
            return createStepZeroResponse(BAD_REQUEST_MSG, null);
        }

        String idTransaction = request.getIdTransaction();
        log.info("START - POST {} for idTransaction {}", VPOS_AUTHORIZATIONS, idTransaction);
        if ((Objects.nonNull(paymentRequestRepository.findByIdTransaction(idTransaction)))) {
            log.warn("Transaction " + idTransaction + " has already been processed previously");
            return createStepZeroResponse(TRANSACTION_ALREADY_PROCESSED_MSG, null);
        }

        try {
            return processStepZero(request, clientId, mdcFields);
        } catch (Exception e) {
            log.error("Error while constructing requestBody for idTransaction {}", idTransaction, e);
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

        log.info("END - POST {} for requestId {}", VPOS_AUTHORIZATIONS, requestId);
        return response;
    }

    private PaymentRequestEntity createEntity(String clientId, String mdcFields, String idTransaction, StepZeroRequest request) throws JsonProcessingException {
        VposPersistableRequest vposPersistableRequest = generateVposDatabaseRequest(request);
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setClientId(clientId);
        entity.setMdcInfo(mdcFields);
        entity.setIdTransaction(idTransaction);
        entity.setGuid(UUID.randomUUID().toString());
        entity.setRequestEndpoint(VPOS_AUTHORIZATIONS);
        entity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        entity.setJsonRequest(vposPersistableRequest);
        entity.setIsFirstPayment(request.getIsFirstPayment());
        return entity;
    }

    private VposPersistableRequest generateVposDatabaseRequest(StepZeroRequest request) {
        return new VposPersistableRequest(request.getIdTransaction(),
                request.getAmount(), request.getIsFirstPayment(), request.getIdPsp());
    }
}
