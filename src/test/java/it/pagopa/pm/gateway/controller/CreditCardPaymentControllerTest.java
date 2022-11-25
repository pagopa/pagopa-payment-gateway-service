package it.pagopa.pm.gateway.controller;

import it.pagopa.pm.gateway.dto.vpos.CcPaymentInfoResponse;
import it.pagopa.pm.gateway.service.CcPaymentInfoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_CREDIT_CARD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CreditCardPaymentController.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class CreditCardPaymentControllerTest {
    @MockBean
    private CcPaymentInfoService ccPaymentInfoService;

    @Autowired
    private MockMvc mvc;

    @Test
    public void getPaymentInfoTest() throws Exception {
        when(ccPaymentInfoService.getPaymentoInfo(any())).thenReturn(new CcPaymentInfoResponse());

        mvc.perform(get(REQUEST_PAYMENTS_CREDIT_CARD + "/123"))
                .andExpect(status().isOk());
    }
}
