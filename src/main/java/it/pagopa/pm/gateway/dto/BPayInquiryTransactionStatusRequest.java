package it.pagopa.pm.gateway.dto;


import lombok.Data;

@Data
public class BPayInquiryTransactionStatusRequest {

    String correlationId;

    Long idPagoPa;

    String language;
}
