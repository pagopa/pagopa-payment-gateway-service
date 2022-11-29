package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VPosRequestUtils.class)
public class VPosRequestUtilsTest {

    @Spy
    @InjectMocks
    private VPosRequestUtils vPosRequestUtils;

    @Before
    public void setUpProperties() {
        ReflectionTestUtils.setField(vPosRequestUtils, "vposResponseUrl", "http://localhost:8080/payment-gateway/");
    }

    @Mock
    VPosUtils vPosUtils;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void generateRequestForStep0_FirstPayment_Test() throws IOException {
        Boolean isFisrtPayment = true;
        Step0CreditCardRequest pgsRequest = ValidBeans.createStep0Request(isFisrtPayment);
        List<String> variables = ValidBeans.generateVariable(isFisrtPayment);
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(variables);
        assertNotNull(vPosRequestUtils.createStepZeroRequest(pgsRequest, "requestId"));
    }

    @Test
    public void generateRequestForStep0_Not_FirstPayment_Test() throws IOException {
        Boolean isFisrtPayment = false;
        Step0CreditCardRequest pgsRequest = ValidBeans.createStep0Request(isFisrtPayment);
        List<String> variables = ValidBeans.generateVariable(isFisrtPayment);
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(variables);
        assertNotNull(vPosRequestUtils.createStepZeroRequest(pgsRequest, "requestId"));
    }

    @Test
    public void generateRequestForAccount_FirstPayment_Test() throws IOException {
        Boolean isFisrtPayment = true;
        Step0CreditCardRequest pgsRequest = ValidBeans.createStep0Request(isFisrtPayment);
        List<String> variables = ValidBeans.generateVariable(isFisrtPayment);
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(variables);
        assertNotNull(vPosRequestUtils.generateRequestForAccount(pgsRequest));
    }

    @Test
    public void generateRequestForAccount_Not_FirstPayment_Test() throws IOException {
        Boolean isFisrtPayment = false;
        Step0CreditCardRequest pgsRequest = ValidBeans.createStep0Request(isFisrtPayment);
        List<String> variables = ValidBeans.generateVariable(isFisrtPayment);
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(variables);
        assertNotNull(vPosRequestUtils.generateRequestForAccount(pgsRequest));
    }

    @Test
    public void generateRequestForRevert_FirstPayment_Test() throws IOException {
        Boolean isFisrtPayment = true;
        Step0CreditCardRequest pgsRequest = ValidBeans.createStep0Request(isFisrtPayment);
        List<String> variables = ValidBeans.generateVariable(isFisrtPayment);
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(variables);
        assertNotNull(vPosRequestUtils.generateRequestForRevert(pgsRequest));
    }

    @Test
    public void generateRequestForRevert_Not_FirstPayment_Test() throws IOException {
        Boolean isFisrtPayment = false;
        Step0CreditCardRequest pgsRequest = ValidBeans.createStep0Request(isFisrtPayment);
        List<String> variables = ValidBeans.generateVariable(isFisrtPayment);
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(variables);
        assertNotNull(vPosRequestUtils.generateRequestForRevert(pgsRequest));
    }

}
