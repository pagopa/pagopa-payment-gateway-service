package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.enums.VposRequestEnum;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.Charset;

public class VPosMacBuilder {
    private static final String AND = "&";
    private static final String EQUAL = "=";
    private final StringBuilder builder = new StringBuilder();

    public void addElement(VposRequestEnum tag, Object value) throws IllegalArgumentException {
        if (value != null) {
            if (builder.length() > 0) {
                builder.append(AND);
            }
            builder.append(tag.getTagName().toUpperCase());
            builder.append(EQUAL);
            builder.append(value);
        }
    }

    public void addString(String value) throws IllegalArgumentException {
        if (value != null) {
            if (builder.length() > 0) {
                builder.append(AND);
            }
            builder.append(value);
        }
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public String toSha1Hex(Charset charset) {
        return DigestUtils.sha1Hex(builder.toString().getBytes(charset));
    }
}
