package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.CardCircuit;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.Shop;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.dto.vpos.VposOrderStatusResponse;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VPosResponseUtils.class)
public class VPosResponseUtilsTest {

    @Spy
    @InjectMocks
    private VPosResponseUtils vPosResponseUtils;

    @Mock
    VPosUtils vPosUtils;

    @Test
    public void validateResponseMac3ds2_Test_OK() {
        ThreeDS2Response threeDS2Response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        StepZeroRequest request = ValidBeans.createStep0Request(true);
        String mac = ValidBeans.createConfigMacStep0(threeDS2Response);
        List<String> variables = ValidBeans.generateVariable(true);
        variables.set(4, mac);
        threeDS2Response.setResultMac(mac);
        Shop shop = ValidBeans.generateShop("321");
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(shop);
        doCallRealMethod().when(vPosResponseUtils).validateResponseMac(threeDS2Response.getTimestamp(),
                threeDS2Response.getResultCode(), threeDS2Response.getResultMac(), request);
        vPosResponseUtils.validateResponseMac(threeDS2Response.getTimestamp(),
                threeDS2Response.getResultCode(), threeDS2Response.getResultMac(), request);
        verify(vPosResponseUtils).validateResponseMac(threeDS2Response.getTimestamp(),
                threeDS2Response.getResultCode(), threeDS2Response.getResultMac(), request);
    }


    @Test
    public void validateResponseMac3ds2_Test_KO() {
        ThreeDS2Response threeDS2Response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        StepZeroRequest request = ValidBeans.createStep0Request(true);
        String mac = ValidBeans.createConfigMacStep0(threeDS2Response);
        List<String> variables = ValidBeans.generateVariable(true);
        variables.set(4, mac);
        Shop shop = ValidBeans.generateShop("321");
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(shop);
        doCallRealMethod().when(vPosResponseUtils).validateResponseMac(threeDS2Response.getTimestamp(),
                threeDS2Response.getResultCode(), threeDS2Response.getResultMac(), request);
        vPosResponseUtils.validateResponseMac(threeDS2Response.getTimestamp(),
                threeDS2Response.getResultCode(), threeDS2Response.getResultMac(), request);
        verify(vPosResponseUtils).validateResponseMac(threeDS2Response.getTimestamp(),
                threeDS2Response.getResultCode(), threeDS2Response.getResultMac(), request);
    }

    @Test
    public void validateResponseMac_Test_OK() {
        AuthResponse response = ValidBeans.createVPosAuthResponse("00");
        StepZeroRequest request = ValidBeans.createStep0Request(false);
        String mac = ValidBeans.createConfigMacAuth(response);
        List<String> variables = ValidBeans.generateVariable(false);
        variables.set(7, mac);
        response.setResultMac(mac);
        Shop shop = ValidBeans.generateShop("321");
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(shop);
        doCallRealMethod().when(vPosResponseUtils).validateResponseMac(response.getTimestamp(),
                response.getResultCode(), response.getResultMac(), request);
        vPosResponseUtils.validateResponseMac(response.getTimestamp(),
                response.getResultCode(), response.getResultMac(), request);
        verify(vPosResponseUtils).validateResponseMac(response.getTimestamp(),
                response.getResultCode(), response.getResultMac(), request);
    }

    @Test
    public void validateResponseMac_Test_KO() {
        AuthResponse response = ValidBeans.createVPosAuthResponse("00");
        StepZeroRequest request = ValidBeans.createStep0Request(false);
        String mac = ValidBeans.createConfigMacAuth(response);
        List<String> variables = ValidBeans.generateVariable(false);
        variables.set(7, mac);
        Shop shop = ValidBeans.generateShop("321");
        when(vPosUtils.getVposShopByIdPsp(any())).thenReturn(shop);
        doCallRealMethod().when(vPosResponseUtils).validateResponseMac(response.getTimestamp(),
                response.getResultCode(), response.getResultMac(), request);
        vPosResponseUtils.validateResponseMac(response.getTimestamp(),
                response.getResultCode(), response.getResultMac(), request);
        verify(vPosResponseUtils).validateResponseMac(response.getTimestamp(),
                response.getResultCode(), response.getResultMac(), request);
    }

    @Test
    public void build3ds2Response_Test_Authorization() throws IOException, JDOMException {
        ThreeDS2Response threeDS2Response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Document document = ValidBeans.createThreeDs2AuthorizationResponseDocument(threeDS2Response);
        byte[] clientResponse = ValidBeans.convertToByte(document);
        ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse);
        assertEquals(threeDS2Response.getResponseType(), response.getResponseType());
        assertEquals(threeDS2Response.getThreeDS2ResponseElement(), response.getThreeDS2ResponseElement());
    }

    @Test
    public void build3ds2Response_Test_Method() throws IOException, JDOMException {
        ThreeDS2Response threeDS2Response = ValidBeans.createThreeDS2ResponseStep0Method();
        Document document = ValidBeans.createThreeDs2MethodResponseDocument(threeDS2Response);
        byte[] clientResponse = ValidBeans.convertToByte(document);
        ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse);
        assertEquals(threeDS2Response.getResponseType(), response.getResponseType());
        assertEquals(threeDS2Response.getThreeDS2ResponseElement(), response.getThreeDS2ResponseElement());
    }

    @Test
    public void build3ds2Response_Test_Challege() throws IOException, JDOMException {
        ThreeDS2Response threeDS2Response = ValidBeans.createThreeDS2ResponseStep0Challenge();
        Document document = ValidBeans.createThreeDs2ChallengeResponseDocument(threeDS2Response);
        byte[] clientResponse = ValidBeans.convertToByte(document);
        ThreeDS2Response response = vPosResponseUtils.build3ds2Response(clientResponse);
        assertEquals(threeDS2Response.getResponseType(), response.getResponseType());
        assertEquals(threeDS2Response.getThreeDS2ResponseElement(), response.getThreeDS2ResponseElement());
    }

    @Test
    public void buildAuthResponse_Test_OK() throws IOException, JDOMException {
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        authResponse.setCircuit(CardCircuit.UNKNOWN);
        Document document = ValidBeans.createAuthResponseDocument(authResponse);
        byte[] clientResponse = ValidBeans.convertToByte(document);
        AuthResponse response = vPosResponseUtils.buildAuthResponse(clientResponse);
        assertEquals(authResponse, response);
    }

    @Test
    public void buildOrderStatusResponse_Test_OK() throws IOException, JDOMException {
        VposOrderStatusResponse testResponse = ValidBeans.createVposOrderStatusResponse("00");
        Document document = ValidBeans.createVposOrderStatusResponseDocument(testResponse);
        byte[] clientResponse = ValidBeans.convertToByte(document);
        VposOrderStatusResponse response = vPosResponseUtils.buildOrderStatusResponse(clientResponse);
        assertEquals(testResponse, response);
    }
}
