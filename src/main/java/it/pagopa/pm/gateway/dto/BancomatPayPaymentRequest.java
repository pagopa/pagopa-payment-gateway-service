package it.pagopa.pm.gateway.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BancomatPayPaymentRequest {

    @NotNull(message = "'idPsp' mandatory")
    String idPsp;

    @NotNull(message = "'idPagoPa' mandatory")
    String idPagoPa;

    @NotNull(message = "'amount' mandatory")
    Double amount;

    String subject;

    @NotNull(message = "'crypted Telephone Number' mandatory")
    String cryptedTelephoneNumber;

//    @NotNull(message = "'tag' mandatory")
//    String tag;

    String language;


}
