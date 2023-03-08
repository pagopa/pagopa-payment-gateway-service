package it.pagopa.pm.gateway.dto.postepay;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PostePayAuthRequest {
    @JsonProperty(required = true)
    private int grandTotal;

    @JsonProperty(required = true)
    private String idTransaction;

    private String name;
    private String emailNotice;
    private String description;
}
