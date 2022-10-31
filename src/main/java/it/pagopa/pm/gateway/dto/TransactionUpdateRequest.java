package it.pagopa.pm.gateway.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionUpdateRequest {

    private Long status;

    private String authCode;

    private Long accountingStatus;

    private String pgsOutcome;

    private String correlationId;

}
