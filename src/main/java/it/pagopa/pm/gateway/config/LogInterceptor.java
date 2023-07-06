package it.pagopa.pm.gateway.config;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.*;
import ch.qos.logback.core.spi.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;

import java.util.*;
import java.util.regex.*;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class LogInterceptor extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String s, Object[] objects, Throwable throwable) {
        if (level.equals(Level.TRACE) || level.equals(Level.DEBUG)) {
            return FilterReply.NEUTRAL;
        }
        boolean logChanged = false;
        if (s != null) {
            String maskedString = maskString(s);
            if (!s.equals(maskedString)) {
                s = maskedString;
                logChanged = true;
            }
        }
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] instanceof String) {
                    String str = (String) objects[i];
                    String maskedString = maskString(str);
                    if (!str.equals(maskedString)) {
                        objects[i] = maskedString;
                        logChanged = true;
                    }
                }
            }
        }
        if (logChanged) {
            logger.log(marker, "", level.toInt(), s, objects, throwable);
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

    private static final String x = "\"\\s*:\\s*\"(.*?)\"";
    private static final String y = "\\w*\"";
    
    private static final List<Pattern> patterns = Arrays.asList(
        Pattern.compile("\"pan" + y + x, CASE_INSENSITIVE),
        Pattern.compile("\\d{16}"),
        Pattern.compile("\"cvv" + y + x, CASE_INSENSITIVE),
        Pattern.compile("\"securityCode" + x, CASE_INSENSITIVE),
        Pattern.compile("\"holder" + y + x, CASE_INSENSITIVE),
        Pattern.compile("\"scadenza" + x, CASE_INSENSITIVE),
        Pattern.compile("\"nome" + x, CASE_INSENSITIVE),
        Pattern.compile("\"cognome" + x, CASE_INSENSITIVE),
        Pattern.compile("\"\\w*exp\\w*date" + y + x, CASE_INSENSITIVE),
        Pattern.compile("([a-z0-9.-]+@[a-z]+\\.[a-z]{2,3})", CASE_INSENSITIVE)
    );

    private String maskString(String string) {
        String maskedString = string;
        for (Pattern p : patterns) {
            maskedString = RegExUtils.replaceAll(maskedString, p, StringUtils.repeat('*', string.length()));
        }
        return maskedString;
    }

}
