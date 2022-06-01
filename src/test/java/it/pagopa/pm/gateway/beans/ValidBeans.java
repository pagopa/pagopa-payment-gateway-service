package it.pagopa.pm.gateway.beans;

import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.dto.enums.EndpointEnum;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.entity.*;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;

public class ValidBeans {

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

    public static PaymentRequestEntity pPayPaymentRequestEntityToFind() {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setIdTransaction(1L);
        paymentRequestEntity.setCorrelationId("id");
        paymentRequestEntity.setGuid("guid");
        paymentRequestEntity.setErrorCode("0");
        paymentRequestEntity.setAuthorizationCode("200");
        paymentRequestEntity.setAuthorizationOutcome(true);
        paymentRequestEntity.setRequestEndpoint(EndpointEnum.POSTEPAY.getValue());
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity pPayPaymentRequestEntityToSave() {
        PaymentRequestEntity paymentRequestEntity = pPayPaymentRequestEntityToFind();
        paymentRequestEntity.setIsProcessed(true);
        return paymentRequestEntity;
    }

    public static AuthMessage authMessage(){
        AuthMessage authMessage = new AuthMessage();
        authMessage.setAuthCode("authCode");
        authMessage.setAuthOutcome(OutcomeEnum.OK);
        return authMessage;
    }

    public static ACKMessage ackMessageResponse(){
        ACKMessage ackMessage = new ACKMessage();
        ackMessage.setOutcome(OutcomeEnum.OK);
        return ackMessage;

    }

    public static ACKError ackGenericErrorResponse() {
        ACKError ackError = new ACKError();
        ackError.setOutcome(OutcomeEnum.KO);
        ackError.setError(ExceptionsEnum.GENERIC_ERROR.getDescription());
        return ackError;
    }

    public static ACKError ackRestapiCdClientErrorResponse() {
        ACKError ackError = new ACKError();
        ackError.setOutcome(OutcomeEnum.KO);
        ackError.setError(ExceptionsEnum.RESTAPI_CD_CLIENT_ERROR.getDescription());
        return ackError;
    }

    public static TransactionUpdateRequest transactionUpdateRequest(){
             TransactionUpdateRequest transactionUpdateRequest = new TransactionUpdateRequest();
             transactionUpdateRequest.setStatus(21L);
             transactionUpdateRequest.setAuthCode("authCode");
             return  transactionUpdateRequest;
    }

    public static BPayRefundRequest bPayRefundRequest(){
        BPayRefundRequest bPayRefundRequest = new BPayRefundRequest();
        bPayRefundRequest.setIdPagoPa(1L);
        bPayRefundRequest.setSubject(null);
        bPayRefundRequest.setLanguage("IT");
        bPayRefundRequest.setRefundAttempt(0);

        return bPayRefundRequest;

    }

    public static InquiryTransactionStatusResponse inquiryTransactionStatusResponse(boolean hasReturn){
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse = new InquiryTransactionStatusResponse();
        ResponseInquiryTransactionStatusVO responseInquiryTransactionStatusVO = new ResponseInquiryTransactionStatusVO();
        responseInquiryTransactionStatusVO.setEsitoPagamento("EFF");
        if (hasReturn)
        inquiryTransactionStatusResponse.setReturn(responseInquiryTransactionStatusVO);

        return inquiryTransactionStatusResponse;

    }

    public static StornoPagamentoResponse stornoPagamentoResponse(boolean hasReturn, boolean esito){
        StornoPagamentoResponse stornoPagamentoResponse = new StornoPagamentoResponse();
        ResponseStornoPagamentoVO responseStornoPagamentoVO = new ResponseStornoPagamentoVO();
        EsitoVO esitoVO = new EsitoVO();
        esitoVO.setEsito(esito);
        responseStornoPagamentoVO.setEsito(esitoVO);
        if (hasReturn)
        stornoPagamentoResponse.setReturn(responseStornoPagamentoVO);

        return stornoPagamentoResponse;
    }


}



