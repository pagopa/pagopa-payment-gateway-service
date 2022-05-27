package it.pagopa.pm.gateway.dto.enums;

import lombok.Getter;

public enum EndpointEnum {

    POSTEPAY("request-payments/postepay");

    @Getter
    private final String value;

    EndpointEnum(String value) {
        this.value = value;
    }

}