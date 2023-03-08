package it.pagopa.pm.gateway.dto.bancomatpay;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class BPayPaymentRequest {

    @NotEmpty(message = "'idPsp' mandatory")
    String idPsp;

    @NotNull(message = "'idPagoPa' mandatory")
    Long idPagoPa;

    @NotNull(message = "'amount' mandatory")
    Double amount;

    String subject;

    @NotEmpty(message = "'encrypted Telephone Number' mandatory")
    String encryptedTelephoneNumber;

    String language;

}
