package it.pagopa.pm.gateway.constant;

import lombok.Data;

@Data(staticConstructor = "of")
public class Profiles {
    public static final String JBOSS_ORACLE = "jboss-oracle";
    public static final String SPRINGBOOT_POSTGRES = "springboot-postgres";
}
