package it.pagopa.pm.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import it.pagopa.pm.gateway.beans.*;
import it.pagopa.pm.gateway.client.bpay.*;
import it.pagopa.pm.gateway.client.bpay.generated.InserimentoRichiestaPagamentoPagoPa;
import it.pagopa.pm.gateway.client.bpay.generated.InserimentoRichiestaPagamentoPagoPaResponse;
import it.pagopa.pm.gateway.client.bpay.generated.ObjectFactory;
import it.pagopa.pm.gateway.constant.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.repository.*;
import org.junit.*;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;
import java.lang.Exception;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PaymentTransactionsController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class ControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BPayPaymentResponseRepository bPayPaymentResponseRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockBean
    private BancomatPayClient client;

    @Mock
    WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

    @Mock
    ObjectFactory objectFactory = new ObjectFactory();

    @Test
    public void givenBancomatPayPaymentRequest_returnBPayPaymentResponseEntity() throws Exception {
        BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        given(client.sendPaymentRequest(request)).willReturn(ValidBeans.inserimentoRichiestaPagamentoPagoPaResponse());
        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_BPAY)
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(ValidBeans.bPayPaymentResponseEntityToReturn())));
        verify(bPayPaymentResponseRepository).save(ValidBeans.bPayPaymentResponseEntityToSave());
        verify(client).sendPaymentRequest(request);
    }

    @Test
    public void givenIncorrectBpayEndopointUrl_shouldReturn5xxStatus() throws Exception {
         BPayPaymentRequest request = ValidBeans.bPayPaymentRequest();
        given(client.sendPaymentRequest(request)).willThrow(Exception.class);
        mvc.perform(post(ApiPaths.REQUEST_PAYMENTS_BPAY)
                .content(mapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

    }
}