package it.pagopa.pm.gateway.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenericUtils {
    public static String maskValue(String value) {
        if(value != null) {
            return value.replaceAll("(?s).", "*");
        }

        return null;
    }
}
