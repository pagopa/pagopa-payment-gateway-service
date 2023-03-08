package it.pagopa.pm.gateway.dto.vpos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import lombok.Data;

@Data
public class ThreeDS2Response {

    private String timestamp;
    private String resultCode;
    private String resultMac;
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "responseType")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = ThreeDS2Authorization.class, name = "AUTHORIZATION"),
            @JsonSubTypes.Type(value = ThreeDS2Method.class, name = "METHOD"),
            @JsonSubTypes.Type(value = ThreeDS2Challenge.class, name = "CHALLENGE")
    })
    private ThreeDS2ResponseElement threeDS2ResponseElement;
    private ThreeDS2ResponseTypeEnum responseType;
}
