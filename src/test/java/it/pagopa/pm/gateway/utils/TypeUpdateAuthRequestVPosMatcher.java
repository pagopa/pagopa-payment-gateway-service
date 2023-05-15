package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.vpos.UpdateAuthRequestVPos;
import org.mockito.ArgumentMatcher;

public class TypeUpdateAuthRequestVPosMatcher implements ArgumentMatcher<UpdateAuthRequestVPos> {
    public boolean matches(UpdateAuthRequestVPos arg) {
        return arg instanceof UpdateAuthRequestVPos;
    }
}