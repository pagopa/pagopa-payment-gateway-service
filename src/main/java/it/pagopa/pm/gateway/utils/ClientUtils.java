package it.pagopa.pm.gateway.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class ClientUtils {

    private ClientUtils(){}

    public static final String INTESA_SP_CODICE_ABI = "03069";

    static final Map<String, String> languageCodeMap;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("IT", "IT");
        map.put("EN", "EN");
        map.put("DE", "DE");
        map.put("it", "IT");
        map.put("en", "EN");
        map.put("de", "DE");
        languageCodeMap = Collections.unmodifiableMap(map);
    }

    public static String getLanguageCode(String code) {
        return languageCodeMap.get(code) != null ? languageCodeMap.get(code) : "IT";
    }

}
