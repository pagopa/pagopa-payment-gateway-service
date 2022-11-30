package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.beans.ValidBeans;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void getVariables_Test_SingleShop() {
        String propertySingleVPosShop = "123|345678|shopdIdF|terminalIdF|macF|shopIdS|terminalIdS|macS";
        given(environment.getProperty("vpos.vposShops")).willReturn(propertySingleVPosShop);
        vPosUtils.getVposShop();
        List<String> variables = ValidBeans.getVariables(propertySingleVPosShop, "123");
        assertEquals(variables, vPosUtils.getVposShopByIdPsp("123"));
    }

    @Test
    public void getVariables_Test_MultipleShop() {
        String propertyMultipleVPosShop = "123|345678|shopdIdF|terminalIdF|macF|shopIdS|terminalIdS|macS*321|589544|shopdIdF2|terminalIdF2|macF2|shopIdS2|terminalIdS2|macS2";
        given(environment.getProperty("vpos.vposShops")).willReturn(propertyMultipleVPosShop);
        vPosUtils.getVposShop();
        List<String> variables = ValidBeans.getVariables(propertyMultipleVPosShop, "321");
        assertEquals(variables, vPosUtils.getVposShopByIdPsp("321"));
    }

    @Test
    public void getVariables_Test_KO() {
        String propertyKOVPosShop = "123|345678|shopdIdF|terminalIdF|macF|shopIdS|terminalIdS|macS*321|589544|shopdIdF2|terminalIdF";
        given(environment.getProperty("vpos.vposShops")).willReturn(propertyKOVPosShop);
        vPosUtils.getVposShop();
        assertNull(vPosUtils.getVposShopByIdPsp("321"));
    }
}
