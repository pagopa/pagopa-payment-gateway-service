package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.DENIED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = EcommercePatchUtils.class)
public class EcommercePatchUtilsTest {

    @Mock
    private EcommerceClient ecommerceClient;

    @Mock
    private ClientsConfig clientsConfig;

    @Spy
    @InjectMocks
    private EcommercePatchUtils ecommercePatchUtils = new EcommercePatchUtils(ecommerceClient, clientsConfig);

    @Test
    public void executePatch_AUTHORIZED_Test() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(AUTHORIZED.name());
        entity.setClientId("clientId");
        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());
        ecommercePatchUtils.executePatchTransaction(entity);
        verify(ecommercePatchUtils).executePatchTransaction(entity);
    }

    @Test
    public void executePatch_DENIED_Test() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(DENIED.name());
        entity.setClientId("clientId");

        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());

        ecommercePatchUtils.executePatchTransaction(entity);
        verify(ecommercePatchUtils).executePatchTransaction(entity);
    }

}
