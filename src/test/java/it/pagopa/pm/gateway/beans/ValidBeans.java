package it.pagopa.pm.gateway.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.XPayOutcomeEnum;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayResponse;
import it.pagopa.pm.gateway.dto.xpay.EsitoXpay;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.entity.PaymentResponseEntity;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.client.model.CreatePaymentRequest;
import org.openapitools.client.model.CreatePaymentResponse;
import org.openapitools.client.model.Esito;
import org.openapitools.client.model.PaymentChannel;
import org.openapitools.client.model.ResponseURLs;
import org.openapitools.client.model.DetailsPaymentResponse;
import org.openapitools.client.model.RefundPaymentResponse;
import org.openapitools.client.model.RefundPaymentRequest;
import org.openapitools.client.model.EsitoStorno;
import org.openapitools.client.model.DetailsPaymentRequest;
import  org.openapitools.client.model.OnboardingRequest;
import org.openapitools.client.model.OnboardingResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_XPAY;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CREATED;
import static org.openapitools.client.model.AuthorizationType.IMMEDIATA;

public class ValidBeans {
    private static final String EURO_ISO_CODE = "978";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RESPONSE_URL_OK_KO = "https://portal.test.pagopa.gov.it/pmmockserviceapi/home";
    public static final String NOTIFICATION_URL = "${postepay.notificationURL}";

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

    public static BPayOutcomeResponse bPayPaymentOutcomeResponseToReturn() {
        BPayOutcomeResponse response = new BPayOutcomeResponse();
        response.setOutcome(true);
        return response;
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

    public static AuthMessage authMessage(OutcomeEnum outcomeEnum) {
        AuthMessage authMessage = new AuthMessage();
        authMessage.setAuthCode("authCode");
        authMessage.setAuthOutcome(outcomeEnum);
        return authMessage;
    }

    public static ACKMessage ackMessageResponse(OutcomeEnum outcomeEnum) {
        ACKMessage ackMessage = new ACKMessage();
        ackMessage.setOutcome(outcomeEnum);
        return ackMessage;
    }

    public static TransactionUpdateRequest transactionUpdateRequest() {
        TransactionUpdateRequest transactionUpdateRequest = new TransactionUpdateRequest();
        transactionUpdateRequest.setStatus(21L);
        transactionUpdateRequest.setAuthCode("authCode");
        transactionUpdateRequest.setPgsOutcome("0");
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

    public static CreatePaymentRequest createPaymentRequest(PaymentChannel paymentChannel) {
        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        String shopId = paymentChannel.equals(PaymentChannel.APP) ? "shopIdTmp_APP" : "shopIdTmp_WEB";
        createPaymentRequest.setMerchantId("merchantId");
        createPaymentRequest.setShopId(shopId);
        createPaymentRequest.setShopTransactionId("1");
        createPaymentRequest.setAmount("1234");
        createPaymentRequest.setDescription("Pagamento bollo auto");
        createPaymentRequest.setCurrency(EURO_ISO_CODE);
        createPaymentRequest.setBuyerName("Mario Rossi");
        createPaymentRequest.setBuyerEmail("mario.rossi@gmail.com");
        createPaymentRequest.setPaymentChannel(paymentChannel);
        createPaymentRequest.setAuthType(IMMEDIATA);

        ResponseURLs responseUrls = new ResponseURLs();
        String urlOkKo = paymentChannel.equals(PaymentChannel.APP) ? StringUtils.EMPTY : RESPONSE_URL_OK_KO;
        responseUrls.setResponseUrlKo(urlOkKo);
        responseUrls.setResponseUrlOk(urlOkKo);
        responseUrls.setServerNotificationUrl(NOTIFICATION_URL);

        createPaymentRequest.setResponseURLs(responseUrls);
        return createPaymentRequest;
    }

    public static OnboardingRequest createOnboardingRequest(PaymentChannel paymentChannel) {
        ResponseURLs responseUrls = new ResponseURLs();
        String urlOkKo = paymentChannel.equals(PaymentChannel.APP) ? StringUtils.EMPTY : RESPONSE_URL_OK_KO;
        responseUrls.setResponseUrlKo(urlOkKo);
        responseUrls.setResponseUrlOk(urlOkKo);
        responseUrls.setServerNotificationUrl(NOTIFICATION_URL);

        OnboardingRequest onboardingRequest = new OnboardingRequest();
        onboardingRequest.setOnboardingTransactionId("1");
        onboardingRequest.setMerchantId("merchantId");
        onboardingRequest.setPaymentChannel(paymentChannel);
        onboardingRequest.setShopId("shopIdTmp_APP");
        onboardingRequest.setResponseURLs(responseUrls);

        return onboardingRequest;
    }


    public static PostePayAuthRequest postePayAuthRequest(boolean isValid) {
        PostePayAuthRequest postePayAuthRequest = new PostePayAuthRequest();
        postePayAuthRequest.setGrandTotal(1234);
        postePayAuthRequest.setIdTransaction(isValid ? "1" : null);
        postePayAuthRequest.setName("Mario Rossi");
        postePayAuthRequest.setEmailNotice("mario.rossi@gmail.com");
        postePayAuthRequest.setDescription("Pagamento bollo auto");
        return postePayAuthRequest;
    }

    public static MicrosoftAzureLoginResponse microsoftAzureLoginResponse() {
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = new MicrosoftAzureLoginResponse();
        microsoftAzureLoginResponse.setAccess_token("access_token");
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

    public static CreatePaymentResponse getOkResponse() {
        CreatePaymentResponse inlineResponse200 = new CreatePaymentResponse();
        inlineResponse200.setPaymentID("1234");
        inlineResponse200.setUserRedirectURL("www.userRedirectUrl.com");
        return inlineResponse200;
    }

    public static OnboardingResponse getOKResponseForOnboarding() {
        OnboardingResponse response = new OnboardingResponse();
        response.setOnboardingID("1234");
        response.setUserRedirectURL("www.userRedirectUrl.com");
        return response;
    }


    public static PaymentRequestEntity paymentRequestEntity(PostePayAuthRequest postePayAuthRequest, Boolean authorizationOutcome, String clientId) {
        String authRequestJson = null;


        if (Objects.nonNull(postePayAuthRequest)) {
            try {
                authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayAuthRequest);
            } catch (JsonProcessingException jspe) {
                jspe.printStackTrace();
            }
        }
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setJsonRequest(authRequestJson);
        paymentRequestEntity.setAuthorizationUrl("www.userRedirectUrl.com");
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        paymentRequestEntity.setIsProcessed(false);
        paymentRequestEntity.setCorrelationId("1234");
        paymentRequestEntity.setAuthorizationCode(null);
        paymentRequestEntity.setIdTransaction("1");
        paymentRequestEntity.setGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        paymentRequestEntity.setId(null);
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setMdcInfo(null);
        paymentRequestEntity.setResourcePath(null);
        paymentRequestEntity.setRequestEndpoint("/request-payments/postepay");
        paymentRequestEntity.setResourcePath("${postepay.logo.url}");
        paymentRequestEntity.setIsOnboarding(false);
        paymentRequestEntity.setStatus("CREATED");
        return paymentRequestEntity;
 }

    public static PaymentRequestEntity paymentRequestEntityOnboarding(PostePayOnboardingRequest postePayOnboardingRequest, Boolean authorizationOutcome, String clientId) {
        String authRequestJson = null;


        if (Objects.nonNull(postePayOnboardingRequest)) {
            try {
                authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayOnboardingRequest);
            } catch (JsonProcessingException jspe) {
                jspe.printStackTrace();
            }
        }
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setJsonRequest(authRequestJson);
        paymentRequestEntity.setAuthorizationUrl("www.userRedirectUrl.com");
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        paymentRequestEntity.setIsProcessed(false);
        paymentRequestEntity.setCorrelationId("1234");
        paymentRequestEntity.setAuthorizationCode(null);
        paymentRequestEntity.setIdTransaction("1");
        paymentRequestEntity.setGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        paymentRequestEntity.setId(null);
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setMdcInfo(null);
        paymentRequestEntity.setResourcePath(null);
        paymentRequestEntity.setRequestEndpoint("/request-payments/postepay");
        paymentRequestEntity.setResourcePath("${postepay.logo.url}");
        paymentRequestEntity.setIsOnboarding(true);
        paymentRequestEntity.setStatus("CREATED");
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity paymentRequestEntityOnboardingFalse(PostePayAuthRequest postePayAuthRequest, Boolean authorizationOutcome, String clientId) {
        PaymentRequestEntity  paymentRequestEntity = paymentRequestEntity(postePayAuthRequest, authorizationOutcome, clientId);
        paymentRequestEntity.setIsOnboarding(false);
        return paymentRequestEntity;
    }

        public static PaymentRequestEntity paymentRequestEntityWithRefundData(String clientId, String authorizationCode, Boolean isRefunded, Boolean changeRequestEndPoint) {
        PaymentRequestEntity paymentRequestEntity = paymentRequestEntity(null, true, clientId);
        paymentRequestEntity.setAuthorizationCode(authorizationCode);
        paymentRequestEntity.setIsRefunded(isRefunded);
        if (changeRequestEndPoint){
            paymentRequestEntity.setRequestEndpoint("/invalidEndpoint");
        }
        return paymentRequestEntity;

    }

    public static PostePayPollingResponse postePayPollingResponse() {
        PostePayPollingResponse postePayPollingResponse = new PostePayPollingResponse();
        postePayPollingResponse.setChannel(PaymentChannel.APP.getValue());
        postePayPollingResponse.setUrlRedirect("www.userRedirectUrl.com");
        postePayPollingResponse.setAuthOutcome(OutcomeEnum.OK);
        postePayPollingResponse.setClientResponseUrl("www.clientResponseUrl.com");
        postePayPollingResponse.setError(StringUtils.EMPTY);
        postePayPollingResponse.setLogoResourcePath("${postepay.logo.url}");
        return postePayPollingResponse;
    }


    public static PostePayPollingResponse postePayPollingResponseError(String error, OutcomeEnum outcomeEnum) {
        PostePayPollingResponse postePayPollingResponse = new PostePayPollingResponse();
        postePayPollingResponse.setChannel(PaymentChannel.APP.getValue());
        postePayPollingResponse.setUrlRedirect("www.userRedirectUrl.com");
        postePayPollingResponse.setAuthOutcome(outcomeEnum);
        postePayPollingResponse.setClientResponseUrl(null);
        postePayPollingResponse.setError(error);
        return postePayPollingResponse;
    }


    public static ResponseEntity<PostePayRefundResponse> postePayRefundResponseResponseEntity(String requestId, String paymentId, String refundOutcome){

        PostePayRefundResponse postePayRefundResponse = new PostePayRefundResponse();
        postePayRefundResponse.setRequestId(requestId);
        postePayRefundResponse.setPaymentId(paymentId);
        postePayRefundResponse.setRefundOutcome(refundOutcome);

        return ResponseEntity.status(HttpStatus.OK).body(postePayRefundResponse);

    }

    public static PostePayRefundResponse postePayRefundResponse(String requestId, String paymentId, String refundOutcome, String error){
        PostePayRefundResponse postePayRefundResponse = new PostePayRefundResponse();
        postePayRefundResponse.setRequestId(requestId);
        postePayRefundResponse.setPaymentId(paymentId);
        postePayRefundResponse.setRefundOutcome(refundOutcome);
        postePayRefundResponse.setError(error);

        return postePayRefundResponse;

    }

    public static DetailsPaymentResponse detailsPaymentResponse(Esito esito){
        DetailsPaymentResponse detailsPaymentResponse = new DetailsPaymentResponse();
        detailsPaymentResponse.setStatus(esito);
        return detailsPaymentResponse;

    }

    public static RefundPaymentResponse refundPaymentResponse(EsitoStorno esitoStorno){
        RefundPaymentResponse refundPaymentResponse = new RefundPaymentResponse();
        refundPaymentResponse.setTransactionResult(esitoStorno);
        return refundPaymentResponse;
    }

    public static RefundPaymentRequest refundPaymentRequest(String authNumber){
        RefundPaymentRequest refundPaymentRequest = new RefundPaymentRequest();
        refundPaymentRequest.setMerchantId("merchantId");
        refundPaymentRequest.setShopId("shopIdTmp_APP");
        refundPaymentRequest.setShopTransactionId("1");
        refundPaymentRequest.setCurrency("978");
        refundPaymentRequest.setPaymentID("1234");
        refundPaymentRequest.setAuthNumber(authNumber);

        return refundPaymentRequest;
    }


    public static DetailsPaymentRequest detailsPaymentRequest(){
        DetailsPaymentRequest detailsPaymentRequest = new DetailsPaymentRequest();
        detailsPaymentRequest.setPaymentID("1234");
        detailsPaymentRequest.setShopTransactionId("1");
        detailsPaymentRequest.setShopId("shopIdTmp_APP");

        return detailsPaymentRequest;
   }

   public static PostePayPatchRequest postePayPatchRequest(){
        PostePayPatchRequest postePayPatchRequest = new PostePayPatchRequest(21L, "authCode", "correlation-ID");
        return postePayPatchRequest;

   }


    public static PostePayOnboardingRequest createPostePayOnboardingRequest(String onboardingTransactionId) {
        PostePayOnboardingRequest postePayOnboardingRequest = new PostePayOnboardingRequest();

        postePayOnboardingRequest.setOnboardingTransactionId(onboardingTransactionId);
        return postePayOnboardingRequest;
    }

    public static XPayAuthRequest createXPayAuthRequest(boolean isValid) {
        XPayAuthRequest xPayAuthRequest = new XPayAuthRequest();
        xPayAuthRequest.setIdTransaction("2");
        xPayAuthRequest.setCvv("123");
        xPayAuthRequest.setPan("1548965265");
        xPayAuthRequest.setExpiryDate("202302");
        xPayAuthRequest.setGrandTotal(isValid ? BigInteger.valueOf(1234) : BigInteger.valueOf(0));
        return xPayAuthRequest;
    }

    public static XPayAuthResponse xPayAuthResponse(boolean isError, String errorMessage, String requestId) {
        XPayAuthResponse xPayAuthResponse = new XPayAuthResponse();
        xPayAuthResponse.setRequestId(requestId);
        if (isError) {
            xPayAuthResponse.setError(errorMessage);
        } else {
            xPayAuthResponse.setUrlRedirect("http://localhost/example.com");
        }
        return xPayAuthResponse;
    }

    public static PaymentRequestEntity paymentRequestEntityxPay(XPayAuthRequest XPayAuthRequest, String clientId, Boolean authorizationOutcome) {
        String authRequestJson = null;


        if (Objects.nonNull(XPayAuthRequest)) {
            try {
                authRequestJson = OBJECT_MAPPER.writeValueAsString(XPayAuthRequest);
            } catch (JsonProcessingException jspe) {
                jspe.printStackTrace();
            }
        }
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(XPayAuthRequest.getIdTransaction());
        paymentRequestEntity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        paymentRequestEntity.setStatus(CREATED.name());
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        if(Objects.nonNull(authorizationOutcome) && authorizationOutcome) {
            paymentRequestEntity.setXpayHtml("<html><body></body></html>");
        }
        return paymentRequestEntity;
    }

    public static  PaymentRequestEntity paymentRequestEntityxPayWithoutHtml(XPayAuthRequest XPayAuthRequest, String clientId, Boolean authorizationOutcome) {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(XPayAuthRequest.getIdTransaction());
        paymentRequestEntity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        paymentRequestEntity.setStatus(CREATED.name());
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        return paymentRequestEntity;
    }

    public static AuthPaymentXPayRequest createAuthPaymentRequest(XPayAuthRequest xPayAuthRequest) throws NoSuchAlgorithmException {
        AuthPaymentXPayRequest authPaymentXPayRequest = new AuthPaymentXPayRequest();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        authPaymentXPayRequest.setApiKey("ExampleApiKey");
        authPaymentXPayRequest.setPan(xPayAuthRequest.getPan());
        authPaymentXPayRequest.setScadenza(xPayAuthRequest.getExpiryDate());
        authPaymentXPayRequest.setCodiceTransazione(xPayAuthRequest.getIdTransaction());
        authPaymentXPayRequest.setImporto(xPayAuthRequest.getGrandTotal());
        authPaymentXPayRequest.setCvv(xPayAuthRequest.getCvv());
        authPaymentXPayRequest.setUrlRisposta("localhost");
        authPaymentXPayRequest.setDivisa("978");
        authPaymentXPayRequest.setTimeStamp(timeStamp);
        authPaymentXPayRequest.setMac(createMac(xPayAuthRequest.getIdTransaction(), xPayAuthRequest.getGrandTotal(), timeStamp));
        return authPaymentXPayRequest;
    }

    public static AuthPaymentXPayResponse createXPayAuthResponse(AuthPaymentXPayRequest authPaymentXPayRequest) {
        AuthPaymentXPayResponse authPaymentXPayResponse = new AuthPaymentXPayResponse();
        authPaymentXPayResponse.setHtml("<html><body></body></html>");
        authPaymentXPayResponse.setEsito(EsitoXpay.OK);
        authPaymentXPayResponse.setTimeStamp(System.currentTimeMillis());
        authPaymentXPayResponse.setMac(authPaymentXPayRequest.getMac());
        return authPaymentXPayResponse;
    }

    public static XPayAuthPollingResponse createXpayAuthPollingResponse(Boolean isOk, XPayPollingResponseError error) {
        XPayAuthPollingResponse response = new XPayAuthPollingResponse();
        if (isOk) {
            response.setHtml("<html><body></body></html>");
            response.setAuthOutcome(XPayOutcomeEnum.OK);
        } else if(Objects.nonNull(error)){
            response.setAuthOutcome(XPayOutcomeEnum.KO);
            response.setError(error);
        } else {
            response.setAuthOutcome(XPayOutcomeEnum.PENDING);
        }
        return response;
    }

    public static PaymentResponseEntity paymentResponseEntityError(XPayPollingResponseError error) {
        PaymentResponseEntity entity = new PaymentResponseEntity();
        entity.setErrorMessage(error.getMessage());
        entity.setErrorCode(error.getCode());
        return entity;
    }

    private static String createMac(String codTrans, BigInteger importo, String timeStamp) throws NoSuchAlgorithmException {
        String macString = String.format("apiKey=%scodiceTransazione=%sdivisa=%simporto=%stimeStamp=%s%s",
                "apiKey", codTrans, "978", importo, timeStamp, "chiavesegreta");
        return hashMac(macString);
    }

    private static String hashMac(String macString) throws NoSuchAlgorithmException {
        String hash = StringUtils.EMPTY;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] in = digest.digest(macString.getBytes(StandardCharsets.UTF_8));

        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        hash = builder.toString();

        return hash;
    }
}



