package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.dto.vpos.Shop;
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

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VPosUtils.class)
public class VPosUtilsTest {

    @Spy
    @InjectMocks
    private VPosUtils vPosUtils;

    @Mock
    private Environment environment;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void getVariables_Test_SingleShop() throws JsonProcessingException {
        String propertySingleVPosShop = "{" +
                "\"shops\": "+
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
        given(environment.getProperty("vpos.vposShops")).willReturn(propertySingleVPosShop);
        vPosUtils.getVposShop();
        assertEquals(shop, vPosUtils.getVposShopByIdPsp("123"));
    }

    @Test
    public void getVariables_Test_MultipleShop() throws JsonProcessingException {
        String propertyMultipleVPosShop = "{" +
                "\"shops\": "+
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
        Shop shop = ValidBeans.generateShop("321");
        given(environment.getProperty("vpos.vposShops")).willReturn(propertyMultipleVPosShop);
        vPosUtils.getVposShop();
        assertEquals(shop, vPosUtils.getVposShopByIdPsp("321"));
    }

    @Test
    public void getVariables_Test_KO() throws JsonProcessingException {
        String propertyKOeVPosShop = "{" +
                "\"shops\": "+
                "[{" +
                "\"idPsp\": \"123\"," +
                "\"shopIdFirstPayment\": \"ShopId_F\"," +
                "\"terminalIdFirstPayment\": \"terminalId_F\"," +
                "\"shopIdSuccPayment\": \"shopId_S\"," +
                "\"macSuccPayment\": \"mac_S\"" +
                "}]" +
                "}";
        given(environment.getProperty("vpos.vposShops")).willReturn(propertyKOeVPosShop);
        vPosUtils.getVposShop();
        assertNull(vPosUtils.getVposShopByIdPsp("123"));
    }
}
