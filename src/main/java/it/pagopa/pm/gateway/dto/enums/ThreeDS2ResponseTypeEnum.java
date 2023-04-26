package it.pagopa.pm.gateway.dto.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum ThreeDS2ResponseTypeEnum {
    METHOD,
    CHALLENGE,
    AUTHORIZATION,
    ERROR;

    public static ThreeDS2ResponseTypeEnum getEnumFromValue(String value) {
        return Arrays.stream(ThreeDS2ResponseTypeEnum.values())
                .filter(enumValue -> StringUtils.equals(enumValue.name(), value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid response type: " + value));
    }
}
