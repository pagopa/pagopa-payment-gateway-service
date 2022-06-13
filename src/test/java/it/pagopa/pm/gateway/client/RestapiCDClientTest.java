package it.pagopa.pm.gateway.client;

import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClient;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestapiCdClientImpl.class)
@AutoConfigureMockMvc
@EnableWebMvc
public class RestapiCDClientTest {

    @MockBean
    private RestapiCdClientImpl restapiCdClient;
    @MockBean
    private RestapiCdClient client;

    @Test
    public void testRestapiCDClientClosePayment () {
        given(client.closePayment(1L, ValidBeans.requestMap(), ValidBeans.headerMap()))
                .willReturn(OutcomeEnum.OK.name());
        given(restapiCdClient.callClosePayment(1L, true, "200"))
                .willReturn(OutcomeEnum.OK.name());
    }

}
