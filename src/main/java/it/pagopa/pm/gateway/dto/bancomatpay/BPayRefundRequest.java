package it.pagopa.pm.gateway.dto.bancomatpay;

import lombok.*;

import javax.validation.constraints.*;

@Data
public class BPayRefundRequest {

    @NotNull(message = "'idPagoPa' mandatory")
    Long idPagoPa;

    String subject;

    String language;

    Integer refundAttempt;

}
