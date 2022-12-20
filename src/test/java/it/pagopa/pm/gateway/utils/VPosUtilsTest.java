package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.dto.vpos.Shop;
import it.pagopa.pm.gateway.dto.vpos.VposShops;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VPosUtils.class)
public class VPosUtilsTest {

    @Spy
    @InjectMocks
    private VPosUtils vPosUtils;

    @Mock
    private Environment environment;

    @Mock
    private ObjectMapper objectMapper;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void getVariables_Test_SingleShop() throws JsonProcessingException {
        String propertySingleVPosShop = "{" +
                "\"shops\": " +
                "[{" +
                "\"idPsp\": \"123\"," +
                "\"abi\": \"ABI\"," +
                "\"shopIdFirstPayment\": \"ShopId_F\"," +
                "\"terminalIdFirstPayment\": \"terminalId_F\"," +
                "\"macFirstPayment\": \"mac_F\"," +
                "\"shopIdSuccPayment\": \"shopId_S\"," +
                "\"terminalIdSuccPayment\": \"terminalId_S\"," +
                "\"macSuccPayment\": \"mac_S\"" +
                "}]" +
                "}";
        Shop shop = ValidBeans.generateShop("123");
        List<Shop> shops = Collections.singletonList(shop);
        VposShops vposShops = new VposShops();
        vposShops.setShops(shops);
        given(environment.getProperty("vpos.vposShops")).willReturn(propertySingleVPosShop);
        when(objectMapper.readValue(anyString(),  any(Class.class))).thenReturn(vposShops);
        vPosUtils.getVposShop();
        assertEquals(shop, vPosUtils.getVposShopByIdPsp("123"));
    }

    @Test
    public void getVariables_Test_MultipleShop() throws IOException {
        String propertyMultipleVPosShop = "{" +
                "\"shops\": " +
                "[{" +
                "\"idPsp\": \"123\"," +
                "\"abi\": \"ABI\"," +
                "\"shopIdFirstPayment\": \"ShopId_F\"," +
                "\"terminalIdFirstPayment\": \"terminalId_F\"," +
                "\"macFirstPayment\": \"mac_F\"," +
                "\"shopIdSuccPayment\": \"shopId_S\"," +
                "\"terminalIdSuccPayment\": \"terminalId_S\"," +
                "\"macSuccPayment\": \"mac_S\"" +
                "}," +
                "{" +
                "\"idPsp\": \"321\"," +
                "\"abi\": \"ABI\"," +
                "\"shopIdFirstPayment\": \"ShopId_F\"," +
                "\"terminalIdFirstPayment\": \"terminalId_F\"," +
                "\"macFirstPayment\": \"mac_F\"," +
                "\"shopIdSuccPayment\": \"shopId_S\"," +
                "\"terminalIdSuccPayment\": \"terminalId_S\"," +
                "\"macSuccPayment\": \"mac_S\"" +
                "}]" +
                "}";
        Shop shop1 = ValidBeans.generateShop("123");
        Shop shop2 = ValidBeans.generateShop("321");
        List<Shop> shops = Arrays.asList(shop1, shop2);
        VposShops vposShops = new VposShops();
        vposShops.setShops(shops);
        given(environment.getProperty("vpos.vposShops")).willReturn(propertyMultipleVPosShop);
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(vposShops);
        vPosUtils.getVposShop();
        assertEquals(shop2, vPosUtils.getVposShopByIdPsp("321"));
    }

    @Test
    public void getVariables_Test_KO() throws JsonProcessingException {
        String propertyKOeVPosShop = "{" +
                "\"shops\": " +
                "[{" +
                "\"idPsp\": \"123\"," +
                "\"shopIdFirstPayment\": \"ShopId_F\"," +
                "\"terminalIdFirstPayment\": \"terminalId_F\"," +
                "\"shopIdSuccPayment\": \"shopId_S\"," +
                "\"macSuccPayment\": \"mac_S\"" +
                "}]" +
                "}";
        Shop shop = ValidBeans.generateKOShop("321");
        List<Shop> shops = Collections.singletonList(shop);
        VposShops vposShops = new VposShops();
        vposShops.setShops(shops);
        given(environment.getProperty("vpos.vposShops")).willReturn(propertyKOeVPosShop);
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(vposShops);
        vPosUtils.getVposShop();
        assertNull(vPosUtils.getVposShopByIdPsp("123"));
    }
}
