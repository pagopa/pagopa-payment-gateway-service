package it.pagopa.pm.gateway.config;

import org.apache.commons.lang3.*;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.*;

@Aspect
@Component
public class NoCachingInController {
        @Around("@annotation(Sensitive)")
        public Object sensitive(ProceedingJoinPoint joinPoint) throws Throwable {
                if (StringUtils.containsIgnoreCase(joinPoint.getSourceLocation().getFileName(), "log")) {
                        throw new RuntimeException("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                }
                return joinPoint.proceed();
        }

}

