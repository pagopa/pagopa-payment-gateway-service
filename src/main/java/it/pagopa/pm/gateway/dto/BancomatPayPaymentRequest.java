package it.pagopa.pm.gateway.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Data
public class BancomatPayPaymentRequest {

    @NotNull(message = "'idPsp' mandatory")
    String idPsp;

    @NotNull(message = "'idPagoPa' mandatory")
    Long idPagoPa;

    @NotNull(message = "'amount' mandatory")
    Double amount;

    String subject;

    @NotNull(message = "'crypted Telephone Number' mandatory")
    String cryptedTelephoneNumber;

    String language;


}
