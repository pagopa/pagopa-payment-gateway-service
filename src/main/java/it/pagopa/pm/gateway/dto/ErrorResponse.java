package it.pagopa.pm.gateway.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private Integer code;

    private String message;

}
