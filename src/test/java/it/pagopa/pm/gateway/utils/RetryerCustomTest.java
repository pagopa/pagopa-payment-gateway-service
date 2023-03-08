package it.pagopa.pm.gateway.utils;

import feign.Request;
import feign.RetryableException;
import feign.Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = RetryerCustom.class)
public class RetryerCustomTest {

    @Spy
    @InjectMocks
    private RetryerCustom retryer = new RetryerCustom(100L, 3000L, 3);

    @Before
    public void setUpProperties() {
        ReflectionTestUtils.setField(retryer, "attempt", 1);
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();


    private final static Request REQUEST = Request
            .create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, Util.UTF_8);

    @Test
    public void cloneTest() {
        retryer.clone();
        Mockito.verify(retryer).clone();
    }

    @Test
    public void continueOrPropagateTest1() {
        RetryableException e = new RetryableException(-1, null, null, null, REQUEST);
        assertEquals(1, retryer.attempt);
        assertEquals(0, retryer.sleptForMillis);

        retryer.continueOrPropagate(e);
        assertEquals(2, retryer.attempt);
        assertEquals(150, retryer.sleptForMillis);

        retryer.continueOrPropagate(e);
        assertEquals(3, retryer.attempt);
        assertEquals(375, retryer.sleptForMillis);

        thrown.expect(RetryableException.class);
        retryer.continueOrPropagate(e);
    }

    @Test
    public void neverRetry() {

        Thread.currentThread().interrupt();
        RetryableException expected =
                new RetryableException(-1, null, null, new Date(System.currentTimeMillis() + 5000),
                        REQUEST);
        try {
            retryer.continueOrPropagate(expected);
            fail("Retryer continued despite interruption");
        } catch (RetryableException e) {
            Assert.assertTrue("Interrupted status not reset", Thread.interrupted());
            Assert.assertEquals("Retry attempt not registered as expected", 2, retryer.attempt);
            Assert.assertEquals("Unexpected exception found", expected, e);
        }
    }
}

