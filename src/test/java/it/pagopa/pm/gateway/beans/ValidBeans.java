package it.pagopa.pm.gateway.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.entity.*;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.client.model.CreatePaymentRequest;
import org.openapitools.client.model.PaymentChannel;
import org.openapitools.client.model.InlineResponse200;

import static org.openapitools.client.model.AuthorizationType.IMMEDIATA;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import org.openapitools.client.model.ResponseURLs;

public class ValidBeans {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static BPayPaymentRequest bPayPaymentRequest() {
        BPayPaymentRequest request = new BPayPaymentRequest();
        request.setIdPsp("Id_psp");
        request.setIdPagoPa(1L);
        request.setAmount(100d);
        request.setSubject("causale");
        request.setEncryptedTelephoneNumber("pqimx8en49fbf");
        request.setLanguage("IT");
        return request;
    }

    public static InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse() {
        InserimentoRichiestaPagamentoPagoPaResponse response = new InserimentoRichiestaPagamentoPagoPaResponse();
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseData = new ResponseInserimentoRichiestaPagamentoPagoPaVO();
        responseData.setCorrelationId("id");
        EsitoVO esitoVO = new EsitoVO();
        esitoVO.setEsito(true);
        esitoVO.setCodice("0");
        esitoVO.setMessaggio("messaggio");
        responseData.setEsito(esitoVO);
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid("7c7c201a-c198-31c7-864c-21d61b2bd810");
        responseData.setContesto(contestoVO);
        response.setReturn(responseData);
        return response;
    }

    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToReturn() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        return entity;
    }

    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToSave() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        entity.setCorrelationId("id");
        entity.setMessage("messaggio");
        entity.setErrorCode("0");
        entity.setClientGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        return entity;
    }

    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToSave_2() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        entity.setCorrelationId("id");
        entity.setMessage("messaggio");
        entity.setErrorCode("0");
        entity.setClientGuid("client-guid");
        entity.setIsProcessed(true);
        return entity;
    }


    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToFind() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        entity.setCorrelationId("id");
        entity.setMessage("messaggio");
        entity.setErrorCode("0");
        entity.setClientGuid("client-guid");
        return entity;
    }

    public static AuthMessage authMessage() {
        AuthMessage authMessage = new AuthMessage();
        authMessage.setAuthCode("authCode");
        authMessage.setAuthOutcome(OutcomeEnum.OK);
        return authMessage;
    }

    public static ACKMessage ackMessageResponse() {
        ACKMessage ackMessage = new ACKMessage();
        ackMessage.setOutcome(OutcomeEnum.OK);
        return ackMessage;

    }

    public static TransactionUpdateRequest transactionUpdateRequest() {
        TransactionUpdateRequest transactionUpdateRequest = new TransactionUpdateRequest();
        transactionUpdateRequest.setStatus(21L);
        transactionUpdateRequest.setAuthCode("authCode");
        return transactionUpdateRequest;
    }

    public static BPayRefundRequest bPayRefundRequest() {
        BPayRefundRequest bPayRefundRequest = new BPayRefundRequest();
        bPayRefundRequest.setIdPagoPa(1L);
        bPayRefundRequest.setSubject(null);
        bPayRefundRequest.setLanguage("IT");
        bPayRefundRequest.setRefundAttempt(0);

        return bPayRefundRequest;

    }

    public static InquiryTransactionStatusResponse inquiryTransactionStatusResponse(boolean hasReturn) {
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse = new InquiryTransactionStatusResponse();
        ResponseInquiryTransactionStatusVO responseInquiryTransactionStatusVO = new ResponseInquiryTransactionStatusVO();
        responseInquiryTransactionStatusVO.setEsitoPagamento("EFF");
        if (hasReturn)
            inquiryTransactionStatusResponse.setReturn(responseInquiryTransactionStatusVO);

        return inquiryTransactionStatusResponse;

    }

    public static StornoPagamentoResponse stornoPagamentoResponse(boolean hasReturn, boolean esito) {
        StornoPagamentoResponse stornoPagamentoResponse = new StornoPagamentoResponse();
        ResponseStornoPagamentoVO responseStornoPagamentoVO = new ResponseStornoPagamentoVO();
        EsitoVO esitoVO = new EsitoVO();
        esitoVO.setEsito(esito);
        responseStornoPagamentoVO.setEsito(esitoVO);
        if (hasReturn)
            stornoPagamentoResponse.setReturn(responseStornoPagamentoVO);

        return stornoPagamentoResponse;
    }

    public static CreatePaymentRequest createPaymentRequest() {
        ResponseURLs responseURLs = new ResponseURLs();
        responseURLs.setResponseUrlKo(StringUtils.EMPTY);
        responseURLs.setResponseUrlOk(StringUtils.EMPTY);
        responseURLs.setServerNotificationUrl("${postepay.notificationURL}");

        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        createPaymentRequest.setShopTransactionId("1");
        createPaymentRequest.setPaymentChannel(PaymentChannel.APP);
        createPaymentRequest.setResponseURLs(responseURLs);
        createPaymentRequest.setAmount("1000");
        createPaymentRequest.setCurrency("978");
        createPaymentRequest.setDescription("causale description");
        createPaymentRequest.setShopId("1");
        createPaymentRequest.setAuthType(IMMEDIATA);
        createPaymentRequest.setBuyerEmail("email@email.com");

        return createPaymentRequest;

    }


    public static PostePayAuthRequest postePayAuthRequest(boolean valid) {
        PostePayAuthRequest postePayAuthRequest = new PostePayAuthRequest();

        postePayAuthRequest.setDescription("causale description");
        postePayAuthRequest.setEmailNotice("email@email.com");
        postePayAuthRequest.setGrandTotal(1000);
        postePayAuthRequest.setName("Username");
        postePayAuthRequest.setIdTransaction(valid?1L:null);

        return postePayAuthRequest;

    }

    public static MicrosoftAzureLoginResponse microsoftAzureLoginResponse() {
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = new MicrosoftAzureLoginResponse();
        microsoftAzureLoginResponse.setAccess_token("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ");
        microsoftAzureLoginResponse.setExpires_in(100);
        microsoftAzureLoginResponse.setToken_type("TOKEN_TYPE");
        microsoftAzureLoginResponse.setExt_expires_in(100);

        return microsoftAzureLoginResponse;

    }

    public static PostePayAuthResponse postePayAuthResponse(String clientId, boolean isError, String errorMessage) {
        PostePayAuthResponse postePayAuthResponse = new PostePayAuthResponse();
        postePayAuthResponse.setChannel(clientId);
        if (isError) {
            postePayAuthResponse.setError(errorMessage);
        } else {
            postePayAuthResponse.setUrlRedirect("${postepay.pgs.response.urlredirect}");
        }

        return postePayAuthResponse;

    }

    public static ResponseEntity<PostePayAuthResponse> postePayAuthResponseResponseEntity(PostePayAuthResponse postePayAuthResponse, HttpStatus status) {
        return ResponseEntity.status(status).body(postePayAuthResponse);
    }

    public static InlineResponse200 inlineResponse200() {
        InlineResponse200 inlineResponse200 = new InlineResponse200();
        inlineResponse200.setPaymentID("1234");
        inlineResponse200.setUserRedirectURL("www.userRedirectUrl.com");
        return inlineResponse200;

    }


    public static PaymentRequestEntity paymentRequestEntity(PostePayAuthRequest postePayAuthRequest) {
        String authRequestJson = null;

        try {
            authRequestJson =OBJECT_MAPPER.writeValueAsString(postePayAuthRequest);
        } catch (JsonProcessingException jspe) {
            jspe.printStackTrace();
        }

        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setJsonRequest(authRequestJson);
        paymentRequestEntity.setAuthorizationUrl("www.userRedirectUrl.com");
        paymentRequestEntity.setAuthorizationOutcome(null);
        paymentRequestEntity.setIsProcessed(false);
        paymentRequestEntity.setCorrelationId("1234");
        paymentRequestEntity.setAuthorizationCode(null);
        paymentRequestEntity.setIdTransaction(1L);
        paymentRequestEntity.setGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        paymentRequestEntity.setId(null);
        paymentRequestEntity.setClientId(null);
        paymentRequestEntity.setMdcInfo(null);
        paymentRequestEntity.setErrorCode(null);
        paymentRequestEntity.setResourcePath(null);
        paymentRequestEntity.setRequestEndpoint("/request-payments/postepay");
        return paymentRequestEntity;


    }

}



