package it.pagopa.pm.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatchRequest {
    private Long status;
    private String authCode;
}
