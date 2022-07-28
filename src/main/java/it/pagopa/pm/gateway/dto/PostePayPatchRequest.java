package it.pagopa.pm.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostePayPatchRequest {
    private Long status;
    private String authCode;
    private String rrn;
}
