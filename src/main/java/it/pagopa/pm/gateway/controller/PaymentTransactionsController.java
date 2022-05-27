package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.pm.gateway.client.bpay.BancomatPayClient;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.client.postepay.PostePayClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.repository.BPayPaymentResponseRepository;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.openapitools.client.api.PaymentManagerControllerApi;
import org.openapitools.client.model.InlineResponse200;
import org.openapitools.client.model.CreatePaymentRequest;
import org.openapitools.client.model.AuthorizationType;
import org.openapitools.client.model.ResponseURLs;
import org.openapitools.client.model.PaymentChannel;
import org.openapitools.client.model.Error;
import org.openapitools.client.ApiException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.lang.Exception;
import java.net.SocketTimeoutException;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.*;
import static it.pagopa.pm.gateway.constant.Headers.*;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.KO;
import static it.pagopa.pm.gateway.dto.enums.OutcomeEnum.OK;
import static it.pagopa.pm.gateway.dto.enums.TransactionStatusEnum.*;
import static it.pagopa.pm.gateway.utils.MdcUtils.setMdcFields;

@RestController
@Slf4j
public class PaymentTransactionsController {



    @Value("${postePay.pgs.response.urlredirect}")
    private String PAYMENT_RESPONSE_URLREDIRECT;

    private static final String INQUIRY_RESPONSE_EFF = "EFF";
    private static final String INQUIRY_RESPONSE_ERR = "ERR";
    private static final String EURO_ISO_CODE = "ISO 978";

    @Autowired
    private BancomatPayClient bancomatPayClient;

    @Autowired
    private PostePayClient postePayClient;

    @Autowired
    private BPayPaymentResponseRepository bPayPaymentResponseRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private RestapiCdClientImpl restapiCdClient;

    @Autowired
    private PaymentManagerControllerApi paymentManagerControllerApi;

    @Autowired
    private Environment env;


    private final ObjectMapper mapper = new ObjectMapper();

    @PutMapping(REQUEST_PAYMENTS_BPAY)
    public ACKMessage updateTransaction(@RequestBody AuthMessage authMessage, @RequestHeader(X_CORRELATION_ID) String correlationId) throws RestApiException {
        MDC.clear();
        log.info("START Update transaction request for correlation-id: " + correlationId + ": " + authMessage);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByCorrelationId(correlationId);
        if (alreadySaved == null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        } else {
            setMdcFields(alreadySaved.getMdcInfo());
            if (Boolean.TRUE.equals(alreadySaved.getIsProcessed())) {
                throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
            }
        }
        TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(authMessage.getAuthOutcome().equals(OK) ? TX_AUTHORIZED_BANCOMAT_PAY.getId() : TX_REFUSED.getId(), authMessage.getAuthCode(), null);
        try {
            restapiCdClient.callTransactionUpdate(alreadySaved.getIdPagoPa(), transactionUpdate);
            alreadySaved.setIsProcessed(true);
            bPayPaymentResponseRepository.save(alreadySaved);
            return new ACKMessage(OK);
        } catch (FeignException fe) {
            log.error("Exception calling RestapiCD to update transaction", fe);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, fe.status());
        } catch (Exception e) {
            log.error("Exception updating transaction", e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } finally {
            log.info("END Update transaction request for correlation-id: " + correlationId);
        }
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENTS_BPAY)
    public BPayPaymentResponseEntity requestPaymentToBancomatPay(@RequestBody BPayPaymentRequest request, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws Exception {
        setMdcFields(mdcFields);
        Long idPagoPa = request.getIdPagoPa();
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (alreadySaved != null) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_ALREADY_PROCESSED);
        }
        log.info("START requestPaymentToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setOutcome(true);
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        executePaymentRequest(request, mdcFields);
        log.info("END requestPaymentToBancomatPay " + idPagoPa);
        return bPayPaymentResponseEntity;
    }

    @Transactional
    @PostMapping(REQUEST_REFUNDS_BPAY)
    public void requestRefundToBancomatPay(@RequestBody BPayRefundRequest request, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws Exception {
        setMdcFields(mdcFields);
        Long idPagoPa = request.getIdPagoPa();

        log.info("START requestRefundToBancomatPay " + idPagoPa);
        BPayPaymentResponseEntity alreadySaved = bPayPaymentResponseRepository.findByIdPagoPa(idPagoPa);
        if (Objects.isNull(alreadySaved)) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }
        String inquiryResponse = inquiryTransactionToBancomatPay(request);
        log.info("Inquiry response for idPagopa " + idPagoPa + ": " + inquiryResponse);
        switch (inquiryResponse) {
            case INQUIRY_RESPONSE_EFF:
                executeRefundRequest(request);
                break;
            case INQUIRY_RESPONSE_ERR:
                break;
            default:
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
    }


    private String inquiryTransactionToBancomatPay(BPayRefundRequest request) throws Exception {
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();

        log.info("START requestInquiryTransactionToBancomatPay " + idPagoPa);

        inquiryTransactionStatusResponse = bancomatPayClient.sendInquiryRequest(request, guid);
        if (inquiryTransactionStatusResponse == null || inquiryTransactionStatusResponse.getReturn() == null) {
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        log.info("END requestInquiryTransactionToBancomatPay " + idPagoPa);
        return inquiryTransactionStatusResponse.getReturn().getEsitoPagamento();

    }


    @Async
    public void executeRefundRequest(BPayRefundRequest request) throws RestApiException {
        StornoPagamentoResponse response;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();

        log.info("START executeRefundRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = bancomatPayClient.sendRefundRequest(request, guid);
            if (response == null || response.getReturn().getEsito() == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            EsitoVO esitoVO = response.getReturn().getEsito();
            log.info("Response from BPay sendRefundRequest - idPagopa: " + idPagoPa + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
            if (Boolean.FALSE.equals(esitoVO.isEsito())) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        log.info("END executeRefundRequest " + idPagoPa);
    }

    @Async
    public void executePaymentRequest(BPayPaymentRequest request, String mdcInfo) throws RestApiException {
        InserimentoRichiestaPagamentoPagoPaResponse response;
        Long idPagoPa = request.getIdPagoPa();
        String guid = UUID.randomUUID().toString();
        log.info("START executePaymentRequest for transaction " + idPagoPa + " with guid: " + guid);
        try {
            response = bancomatPayClient.sendPaymentRequest(request, guid);
            if (response == null || response.getReturn() == null || response.getReturn().getEsito() == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            EsitoVO esitoVO = response.getReturn().getEsito();
            log.info("Response from BPay sendPaymentRequest - idPagopa: " + idPagoPa + " - correlationId: " + response.getReturn().getCorrelationId() + " - esito: " + esitoVO.getCodice() + " - messaggio: " + esitoVO.getMessaggio());
        } catch (Exception e) {
            log.error("Exception calling BancomatPay with idPagopa: " + idPagoPa, e);
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new RestApiException(ExceptionsEnum.TIMEOUT);
            }
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        BPayPaymentResponseEntity bPayPaymentResponseEntity = convertBpayPaymentResponseToEntity(response, idPagoPa, guid, mdcInfo);
        bPayPaymentResponseRepository.save(bPayPaymentResponseEntity);
        try {
            TransactionUpdateRequest transactionUpdate = new TransactionUpdateRequest(TX_PROCESSING.getId(), null, null);
            restapiCdClient.callTransactionUpdate(idPagoPa, transactionUpdate);
        } catch (FeignException e) {
            log.error("Exception calling RestapiCD transaction update", e);
            throw new RestApiException(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR, e.status());
        }
        log.info("END executePaymentRequest for transaction " + idPagoPa);
    }

    private BPayPaymentResponseEntity convertBpayPaymentResponseToEntity(InserimentoRichiestaPagamentoPagoPaResponse response, Long idPagoPa, String guid, String mdcInfo) {
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseReturnVO = response.getReturn();
        EsitoVO esitoVO = responseReturnVO.getEsito();
        BPayPaymentResponseEntity bPayPaymentResponseEntity = new BPayPaymentResponseEntity();
        bPayPaymentResponseEntity.setIdPagoPa(idPagoPa);
        bPayPaymentResponseEntity.setOutcome(esitoVO.isEsito());
        bPayPaymentResponseEntity.setMessage(esitoVO.getMessaggio());
        bPayPaymentResponseEntity.setErrorCode(esitoVO.getCodice());
        bPayPaymentResponseEntity.setCorrelationId(responseReturnVO.getCorrelationId());
        bPayPaymentResponseEntity.setClientGuid(guid);
        bPayPaymentResponseEntity.setMdcInfo(mdcInfo);
        return bPayPaymentResponseEntity;
    }

    @Transactional
    @PostMapping(REQUEST_PAYMENT_POSTEPAY)
    public ResponseEntity<PostePayAuthResponse> requestPaymentPostepay(@RequestBody PostePayAuthRequest postePayAuthRequest, @RequestHeader(value = CLIENT_ID) String clientId, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws RestApiException {
        setMdcFields(mdcFields);

        if (ObjectUtils.anyNull(postePayAuthRequest.getGrandTotal(), postePayAuthRequest.getTransactionId())) {
           return  createPostePayAuthResponse(clientId,"Bad Request - mandatory parameters missing", true, HttpStatus.BAD_REQUEST);
      }

        Long idTransaction = postePayAuthRequest.getTransactionId();
        PaymentRequestEntity alreadySaved = paymentRequestRepository.findByIdTransaction(idTransaction);

        if (alreadySaved != null) {
            return  createPostePayAuthResponse(clientId,"Transaction already processed", true, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("START requestPaymentPostepay " + idTransaction);
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENT_POSTEPAY);
        paymentRequestEntity.setIdTransaction(idTransaction);
        paymentRequestEntity.setMdcInfo(mdcFields);

        String json;
        try {
            json = mapper.writeValueAsString(postePayAuthRequest);
            log.debug("Resulting postePayAuthRequest JSON string = " + json);
        } catch (JsonProcessingException e) {
           //togliere printStackTrace e aggiungere log.error ("message", e)
           log.error("Error while creating json from PostePayAuthRequest object", e);
           return  createPostePayAuthResponse(clientId,"Error while creating json from PostePayAuthRequest object", true, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        paymentRequestEntity.setJsonRequest(json);

        try {
            executePostePayPayment(postePayAuthRequest, clientId, paymentRequestEntity);
        } catch (Exception e){
            return createPostePayAuthResponse(clientId,"Error while executing payment for idTransaction " + idTransaction, true, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("END requestPaymentPostepay " + idTransaction);

        return createPostePayAuthResponse(clientId,null, false, HttpStatus.OK);

    }

    @GetMapping(REQUEST_PAYMENT_POSTEPAY_REQUEST_ID)
    @ResponseBody
    public PostePayPollingResponse getPaymentPostepayResponse(@PathVariable String requestId, @RequestHeader(required = false, value = MDC_FIELDS) String mdcFields) throws RestApiException {
        setMdcFields(mdcFields);
        PaymentRequestEntity request = paymentRequestRepository.findByGuid(requestId);
        if (request == null || !REQUEST_PAYMENT_POSTEPAY.equals(request.getRequestEndpoint())) {
            throw new RestApiException(ExceptionsEnum.TRANSACTION_NOT_FOUND);
        }
        return new PostePayPollingResponse(
                request.getClientId(),
                request.getAuthorizationUrl(),
                Boolean.TRUE.equals(request.getAuthorizationOutcome()) ? OK : KO,
                request.getErrorCode()
        );
    }

    @Async
    private void executePostePayPayment(PostePayAuthRequest postePayAuthRequest, String clientId, PaymentRequestEntity paymentRequestEntity)
            throws RestApiException, JsonProcessingException {
        Long idTransaction = postePayAuthRequest.getTransactionId();
        log.info("START executePostePayPayment for transaction " + idTransaction);

        CreatePaymentRequest createPaymentRequest = mapPostePayAuthRequestToCreatePaymentRequest(postePayAuthRequest, clientId);
        InlineResponse200 inlineResponse200;

        try {
            MicrosoftAzureLoginResponse microsoftAzureLoginResponse = postePayClient.requestMicrosoftAzureLogin();
            String bearerTokenAuthorization = "Bearer " + microsoftAzureLoginResponse.getAccess_token();
            inlineResponse200 = paymentManagerControllerApi.apiV1PaymentCreatePost(bearerTokenAuthorization, createPaymentRequest);
            if (inlineResponse200 == null) {
                throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
            }
            log.info("Response from PostePay createPayment - idTransaction: " + idTransaction + " - paymentID: "
                    + inlineResponse200.getPaymentID() + " - userRedirectUrl: " + inlineResponse200.getUserRedirectURL());
        } catch (ApiException e) {
            Error error = mapper.readValue(e.getResponseBody(), Error.class);
            log.error("Error from PostePay createPayment: " + error);
            updateAndSavePaymentRequestEntity(paymentRequestEntity, false, null, Integer.toString(e.getCode()), null);

            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("Exception while calling Postepay - setting AuthorizationOutcome to false - idTransaction " + idTransaction , e);
            updateAndSavePaymentRequestEntity(paymentRequestEntity, false, null, null, null);

            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("SocketTimeoutException during Postepay calling");
                throw new RestApiException(ExceptionsEnum.TIMEOUT);
            }
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        updateAndSavePaymentRequestEntity(paymentRequestEntity, true, inlineResponse200.getPaymentID(),
                null, inlineResponse200.getUserRedirectURL());

        log.info("END executePostePayPayment for transaction" + idTransaction);
    }

       private void updateAndSavePaymentRequestEntity(PaymentRequestEntity paymentRequestEntity, boolean authorizationOutcome,
                                                      String correlationId, String errorCode,  String authorizationUrl){
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        paymentRequestEntity.setCorrelationId(correlationId);
        paymentRequestEntity.setErrorCode(errorCode);
        paymentRequestEntity.setAuthorizationUrl(authorizationUrl);
        paymentRequestRepository.save(paymentRequestEntity);

       }

        private CreatePaymentRequest mapPostePayAuthRequestToCreatePaymentRequest (PostePayAuthRequest
        postePayAuthRequest, String clientId){

            String configs = env.getProperty("postePay.clientId." + clientId + ".config");

            List<String> configsList = getConfigValues(configs);

            CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
            createPaymentRequest.setAmount(String.valueOf(postePayAuthRequest.getGrandTotal()));
            createPaymentRequest.setPaymentChannel(PaymentChannel.valueOf(configsList.get(1)));
            createPaymentRequest.setAuthType(AuthorizationType.fromValue(configsList.get(2)));
            createPaymentRequest.setBuyerEmail(postePayAuthRequest.getEmailNotice());
            createPaymentRequest.setCurrency(EURO_ISO_CODE);
            createPaymentRequest.setDescription(postePayAuthRequest.getDescription());
            createPaymentRequest.setShopId(configsList.get(0));

            ResponseURLs responseURLs = new ResponseURLs();
            setResponseUrl(responseURLs, clientId, configsList.get(3));
            createPaymentRequest.setResponseURLs(responseURLs);

            createPaymentRequest.setShopTransactionId(postePayAuthRequest.getTransactionId().toString());

            return createPaymentRequest;

        }

        private void setResponseUrl (ResponseURLs responseURLs, String clientId, String responseUrl){

            switch (clientId) {
                case "APP":
                    responseURLs.setResponseUrlOk("");
                    responseURLs.setResponseUrlKo("");
                    responseURLs.setServerNotificationUrl("url della put");
                    break;
                case "WEB":
                    responseURLs.setResponseUrlOk(responseUrl);
                    responseURLs.setResponseUrlKo(responseUrl);
                    responseURLs.setServerNotificationUrl("url della put");
                    break;
                default:
                    break;
            }

        }

        private List<String> getConfigValues (String config){
            StringTokenizer tokenizer = new StringTokenizer(config, "|", false);
            List<String> configs = new ArrayList<>();

            while (tokenizer.hasMoreTokens()) {
                configs.add(tokenizer.nextToken());
            }
            return configs;

        }

        private ResponseEntity<PostePayAuthResponse> createPostePayAuthResponse (String channel, String error,
        boolean isError, HttpStatus status){

            PostePayAuthResponse postePayAuthResponse = new PostePayAuthResponse();
            postePayAuthResponse.setChannel(channel);
            if (isError) {
                postePayAuthResponse.setError(error);
            } else {
                postePayAuthResponse.setUrlRedirect(PAYMENT_RESPONSE_URLREDIRECT);
            }
            return ResponseEntity.status(status).body(postePayAuthResponse);

        }


    }
