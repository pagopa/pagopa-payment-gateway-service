package it.pagopa.pm.gateway.client.restapicd;

import feign.*;
import feign.jackson.*;
import it.pagopa.pm.gateway.dto.*;
import lombok.extern.log4j.*;
import org.apache.commons.lang3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

import javax.annotation.*;

@Log4j2
@Component
public class RestapiCdClientImpl {

    @Value("${HOSTNAME}")
    public String hostname;

    @Value("${HOSTNAME_PM}")
    public String hostnamePm;

    @PostConstruct
    public void init() {
        restapiCdClient = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(RestapiCdClient.class, StringUtils.firstNonBlank(hostname, hostnamePm));
    }

    private RestapiCdClient restapiCdClient;

    @Async
    public void callTransactionUpdate(Long id, TransactionUpdateRequest request) {
        log.info("Calling PM...");
        restapiCdClient.updateTransaction(id, request);
    }

}
