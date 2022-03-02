package it.pagopa.pm.gateway.utils;

import java.util.HashMap;
import java.util.Map;

public class ClientUtil {

    public static final String intesaSPCodiceAbi = "03069";

    static final Map<String, String> languageCodeMap = new HashMap<String, String>() {{
        put("IT", "IT");
        put("EN", "EN");
        put("DE", "DE");
        put("it", "IT");
        put("en", "EN");
        put("de", "DE");
    }};

    public static String getLanguageCode(String code){
        return languageCodeMap.get(code)!=null?languageCodeMap.get(code):"IT";
    }

}
