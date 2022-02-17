package it.pagopa.pm.gateway.dto;

import lombok.Data;

@Data
public class BancomatPayPaymentResponse {

    String outcome;

    String errorCode;

    String message;

    String correlationId;

}
