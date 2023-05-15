package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.xpay.UpdateAuthRequestXPay;
import org.mockito.ArgumentMatcher;

public class TypeUpdateAuthRequestXPayMatcher implements ArgumentMatcher<UpdateAuthRequestXPay> {
    public boolean matches(UpdateAuthRequestXPay arg) {
        return arg instanceof UpdateAuthRequestXPay;
    }
}