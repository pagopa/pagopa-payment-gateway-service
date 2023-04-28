package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.config.VposClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.async.VposAsyncService;
import it.pagopa.pm.gateway.utils.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pm.gateway.constant.Messages.BAD_REQUEST_MSG_CLIENT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VposService.class)
public class VposServiceTest {
    public static final String ECOMMERCE_WEB = "ECOMMERCE_WEB";
    private final ClientConfig clientConfig = new ClientConfig();

    @Mock
    private PaymentRequestRepository paymentRequestRepository;
    @Mock
    private EcommerceClient ecommerceClient;
    @Mock
    private VPosRequestUtils vPosRequestUtils;
    @Mock
    private VPosResponseUtils vPosResponseUtils;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ClientsConfig clientsConfig;
    @Mock
    private JwtTokenUtils jwtTokenUtils;
    @Mock
    private EcommercePatchUtils ecommercePatchUtils;

    @Spy
    @InjectMocks
    private VposService service;

    @Spy
    @InjectMocks
    private VposAsyncService asyncService;

    @Before
    public void init() {
        ReflectionTestUtils.setField(service, "vposPollingUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(asyncService, "vposUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "vposAsyncService", asyncService);

        VposClientConfig vposClientConfig = new VposClientConfig();
        vposClientConfig.setClientReturnUrl("url");
        clientConfig.setVpos(vposClientConfig);
    }

    @Test
    public void getRequestPayment_Invalid_ClientId_Test_400() {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);

        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.BAD_REQUEST, null);
        mockResponse.setError(BAD_REQUEST_MSG_CLIENT_ID);
        when(clientsConfig.containsKey(any())).thenReturn(false);

        StepZeroResponse realResponse = service.startCreditCardPayment("invalido", null, requestOK);
        assertEquals(mockResponse, realResponse);
    }

    @Test
    public void getRequestPayment_Invalid_Request_Test_400() {
        StepZeroRequest request = ValidBeans.createStep0Request(false);
        request.setAmount(BigInteger.ZERO);
        when(clientsConfig.containsKey(any())).thenReturn(true);

        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.BAD_REQUEST, null);

        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, request);
        assertEquals(mockResponse, realResponse);
    }

    @Test
    public void getRequestPayment_Invalid_IdTransaction_Test_401() {
        StepZeroRequest request = ValidBeans.createStep0Request(false);
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guidProvaRandoma");
        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(entity);

        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.UNAUTHORIZED, null);

        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, request);
        assertEquals(mockResponse, realResponse);
    }

    @Test
    public void getRequestPayment_No_FirstPayment_Test_OK() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }


    @Test
    public void getRequestPayment_FirstPayment_Test_200() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }

    @Test
    public void getRequestPayment_Test_500() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenThrow(RuntimeException.class);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.INTERNAL_SERVER_ERROR, requestOK.getIdTransaction());
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertEquals(mockResponse.getError(), realResponse.getError());
    }

    @Test
    public void getRequestPayment_Error_During_Payment_Test_200() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createKOHttpClientResponseVPos());
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }

    @Test
    public void getRequestPayment_Error_During_Account_Test_200() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos()).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }

    @Test
    public void getRequestPayment_Error_During_Revert_Test_200() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos()).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }

    @Test
    public void getRequestPayment_Not_Authorized_METHOD_Test_OK() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Method();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }

    @Test
    public void getRequestPayment_Not_Authorized_CHALLENGE_Test_OK() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Challenge();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }

    @Test
    public void getRequestPayment_Status_Denied_Test_OK() throws Exception {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        response.setResultCode("32");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.containsKey(any())).thenReturn(true);
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse realResponse = service.startCreditCardPayment(ECOMMERCE_WEB, null, requestOK);
        assertNotNull(realResponse.getRequestId());
    }
}
