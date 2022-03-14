package it.pagopa.pm.gateway.ExceptionUtil;

import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public final class ExceptionEnumMatcher {

        public static Matcher<RestApiException> withExceptionEnum(Matcher<ExceptionsEnum> nMatcher) {
            return new FeatureMatcher<RestApiException, ExceptionsEnum>(nMatcher, "exceptionsEnum", "exceptionsEnum") {
                @Override
                protected ExceptionsEnum featureValueOf(RestApiException actual) {
                    return actual.getExceptionsEnum();
                }
            };
        }
}
