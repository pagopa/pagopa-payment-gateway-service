package it.pagopa.pm.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.constant.Headers;
import it.pagopa.pm.gateway.dto.creditcard.CreditCardResumeRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.service.CcPaymentInfoService;
import it.pagopa.pm.gateway.service.CcResumeStep1Service;
import it.pagopa.pm.gateway.service.CcResumeStep2Service;
import it.pagopa.pm.gateway.service.VposService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CreditCardPaymentController.class)
@AutoConfigureMockMvc
@EnableWebMvc
@TestPropertySource(properties = {
        "vpos.response.urlredirect=http://localhost:8080/",
})
public class CreditCardPaymentControllerTest {
    @MockBean
    private CcPaymentInfoService ccPaymentInfoService;

    @MockBean
    private VposService vposService;

    @MockBean
    private CcResumeStep1Service resumeStep1Service;

    @MockBean
    private CcResumeStep2Service resumeStep2Service;

    @Mock
    private Environment environment;

    @Autowired
    private MockMvc mvc;

    @Test
    public void getPaymentInfoTest() throws Exception {
        when(ccPaymentInfoService.getPaymentoInfo(any())).thenReturn(new CcPaymentInfoResponse());

        mvc.perform(get(REQUEST_PAYMENTS_CREDIT_CARD + "/123"))
                .andExpect(status().isOk());
    }

    private static final String APP_ORIGIN = "APP";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void startCreditCardPayment_Test() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        StepZeroResponse stepZeroResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        when(vposService.startCreditCardPayment(any(), any(), any())).thenReturn(stepZeroResponse);

        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(requestOK))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void startCreditCardPayment_Test_400() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        StepZeroResponse stepZeroResponse = ValidBeans.createStepzeroResponse(HttpStatus.BAD_REQUEST, null);
        when(vposService.startCreditCardPayment(any(), any(), any())).thenReturn(stepZeroResponse);

        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(requestOK))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void startCreditCardPayment_Test_401() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        StepZeroResponse stepZeroResponse = ValidBeans.createStepzeroResponse(HttpStatus.UNAUTHORIZED, null);
        when(vposService.startCreditCardPayment(any(), any(), any())).thenReturn(stepZeroResponse);

        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(requestOK))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void startCreditCardPayment_Test_500() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        StepZeroResponse stepZeroResponse = ValidBeans.createStepzeroResponse(HttpStatus.INTERNAL_SERVER_ERROR, null);
        when(vposService.startCreditCardPayment(any(), any(), any())).thenReturn(stepZeroResponse);

        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD)
                        .header(Headers.X_CLIENT_ID, APP_ORIGIN)
                        .content(mapper.writeValueAsString(requestOK))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void resumeCreditCardPayment_Step1_Test_302() throws Exception {
        CreditCardResumeRequest request = ValidBeans.createCreditCardResumeRequest(true);
        String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";
        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD + "/" + UUID_SAMPLE + "/resume/method")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void resumeCreditCardPayment_Step2_Test_302() throws Exception {
        String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";
        mvc.perform(post(REQUEST_PAYMENTS_CREDIT_CARD + "/" + UUID_SAMPLE + "/resume/challenge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFound());
    }

}
