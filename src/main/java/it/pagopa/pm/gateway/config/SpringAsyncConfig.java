package it.pagopa.pm.gateway.config;

import it.pagopa.pm.gateway.constant.Profiles;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@Profile(Profiles.ASYNC_METHOD_ENABLED)
public class SpringAsyncConfig  {

}