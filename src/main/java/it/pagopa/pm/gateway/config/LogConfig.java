package it.pagopa.pm.gateway.config;

import ch.qos.logback.classic.*;
import org.slf4j.*;
import org.springframework.context.annotation.*;

@Configuration
public class LogConfig {

    @Bean
    public LogInterceptor logInterceptor() {
        LogInterceptor logInterceptor = new LogInterceptor();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.addTurboFilter(logInterceptor);
        return logInterceptor;
    }

}
