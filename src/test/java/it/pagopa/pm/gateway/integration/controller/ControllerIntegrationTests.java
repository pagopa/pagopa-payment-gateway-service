package it.pagopa.pm.gateway.integration.controller;

import it.pagopa.pm.gateway.*;
import org.junit.jupiter.api.Test;

import org.junit.runner.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerIntegrationTests {

	@Autowired
	private TestRestTemplate template;

    @Test
    void test() {
        ResponseEntity<String> response = template.getForEntity("/test", String.class);
        assertThat(response.getBody()).isEqualTo("test");
    }

}