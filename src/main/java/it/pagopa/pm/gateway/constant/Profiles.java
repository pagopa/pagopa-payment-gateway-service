package it.pagopa.pm.gateway.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Profiles {
    public static final String JBOSS_ORACLE = "jboss-oracle";
    public static final String SPRINGBOOT_POSTGRES = "springboot-postgres";
}
