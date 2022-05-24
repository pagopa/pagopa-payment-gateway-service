package it.pagopa.pm.gateway.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class PostePayAuthRequest {

    @NotNull(message = "'grandTotal' mandatory")
    Double grandTotal;

    @NotNull(message = "'transactionId' mandatory")
    Long transactionId;

    @NotEmpty(message = "'paymentChannel' mandatory")
    String paymentChannel;

    @NotEmpty(message = "'name' mandatory")
    String name;

    @NotEmpty(message = "'emailNotice' mandatory")
    String emailNotice;

}
