package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;

import java.util.*;

import static it.pagopa.pm.gateway.constant.Headers.MDC_FIELDS;

@Slf4j
public class MdcUtils {

    private MdcUtils(){}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void setMdcFields(String mdcFields) {
        try {
            MDC.clear();
            if (StringUtils.isNotBlank(mdcFields)) {
                objectMapper.readValue(new String(Base64.decodeBase64(mdcFields)), new TypeReference<HashMap<String, String>>() {}).forEach(MDC::put);
            }
        } catch (Exception e) {
            log.error("Exception parsing MDC fields. Raw data: " + mdcFields, e);
        }
    }

    public static Map<String, Object> buildMdcHeader() {
        Map<String, Object> headerMap = new HashMap<>();
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        try {
            if (mdc != null) {
                headerMap.put(MDC_FIELDS, Base64.encodeBase64String(objectMapper.writeValueAsString(mdc).getBytes()));
            }
        } catch (Exception e) {
            log.error("Exception setting MDC header, raw data: " + mdc, e);
        }
        return headerMap;
    }

}
