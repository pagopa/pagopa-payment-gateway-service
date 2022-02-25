package it.pagopa.pm.gateway.client.restapicd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class RestapiCdClient {

    @Autowired
    RestTemplate restTemplatePoolConnection;

}
