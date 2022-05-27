package it.pagopa.pm.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PostePayAuthRequest {
    @JsonProperty(required = true)
    private int grandTotal;

    @JsonProperty(required = true)
    private Long idTransaction;

    private String name;
    private String emailNotice;
    private String description;
}
