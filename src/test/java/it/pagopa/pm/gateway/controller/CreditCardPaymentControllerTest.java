package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.constant.Headers;
import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardRequest;
import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardResponse;
import it.pagopa.pm.gateway.service.CCRequestPaymentsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CreditCardPaymentController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class CreditCardPaymentControllerTest {

    @MockBean
    private CCRequestPaymentsService ccRequestPaymentsService;

    @Autowired
    private MockMvc mvc;

    private static final String APP_ORIGIN = "APP";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void getPaymentInfoTest() throws Exception {
        Step0CreditCardRequest requestOK = ValidBeans.createStep0Request();
        when(ccRequestPaymentsService.getRequestPayments(any(), any(), any())).thenReturn(ResponseEntity.ok().body(new Step0CreditCardResponse()));

        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD)
                .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                .content(mapper.writeValueAsString(requestOK))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
