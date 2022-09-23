package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.constant.ApiPaths;
import lombok.extern.slf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(ApiPaths.HEALTHCHECK)
public class HealthCheckController {

    @GetMapping
    public ResponseEntity healthcheck() {
        try {
            log.debug("pagopa-payment-transaction-gateway healthcheck");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Exception calling healthcheck controller", e);
            throw e;
        }
    }

}
