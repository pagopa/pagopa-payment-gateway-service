package it.pagopa.pm.gateway.dto;

import lombok.*;

import javax.validation.constraints.*;

@Data
public class BPayRefundRequest {

    @NotNull(message = "'idPagoPa' mandatory")
    Long idPagoPa;

    @NotNull(message = "'endToEndId' mandatory")
    String endToEndId;

    String subject;

    String language;

}
