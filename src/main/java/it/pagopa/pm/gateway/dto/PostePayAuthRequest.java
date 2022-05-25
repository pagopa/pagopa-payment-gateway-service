package it.pagopa.pm.gateway.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PostePayAuthRequest {

    @NotNull(message = "'grandTotal' mandatory")
    int grandTotal;

    @NotNull(message = "'transactionId' mandatory")
    Long transactionId;

    String name;

    String emailNotice;

    String description;

}
