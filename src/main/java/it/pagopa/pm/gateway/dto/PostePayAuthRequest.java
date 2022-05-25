package it.pagopa.pm.gateway.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class PostePayAuthRequest {

    @NotNull(message = "'grandTotal' mandatory")
    int grandTotal;

    @NotNull(message = "'transactionId' mandatory")
    Long transactionId;

    @NotEmpty(message = "'paymentChannel' mandatory")
    String paymentChannel;

    String name;

    String emailNotice;

}
