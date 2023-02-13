package it.pagopa.pm.gateway.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = JwtTokenUtils.class)
public class JwtTokenUtilsTest {

    @Spy
    @InjectMocks
    private JwtTokenUtils jwtTokenUtils;

    @Test
    public void generateToken_TEST_OK() {
        String requestId = UUID.randomUUID().toString();
        assertNotNull(jwtTokenUtils.generateToken(requestId));
    }
}
