package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.beans.ValidBeans;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VPosUtils.class)
public class VPosUtilsTest {

    private final String propertyVPosShop = "123|345678|shopdIdF|terminalIdF|macF|shopIdS|terminalIdS|macS";

    @Spy
    @InjectMocks
    private VPosUtils vPosUtils;

    @Before
    public void setUpProperties() {
        ReflectionTestUtils.setField(vPosUtils, "vposShops", propertyVPosShop);
        vPosUtils.getVposShop();
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void getVariables_Test() {
        List<String> variables = ValidBeans.getVariables(propertyVPosShop);
        assertEquals(variables, vPosUtils.getVposShopByIdPsp("123"));
    }
}
