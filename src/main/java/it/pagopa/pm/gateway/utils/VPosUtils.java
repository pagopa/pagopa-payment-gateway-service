package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.vpos.Shop;
import it.pagopa.pm.gateway.dto.vpos.VposShops;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class VPosUtils {

    private final Map<String, Shop> vposShopMap = new HashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private Environment environment;

    /*
     * A VPos shop is identified by the following parameters:
     * idPsp, ABI, shopId, terminalId, MAC
     * where shopId, terminalId and MAC are different for first or subsequent (payments.
     */
    @PostConstruct
    public void getVposShop() throws JsonProcessingException {
        String vposShopsString = environment.getProperty("vpos.vposShops");
        if (vposShopsString != null) {
            VposShops configShops = getConfigShops(vposShopsString);
            List<Shop> allShops = configShops.getShops();
            for (Shop shop : allShops) {
                String idPsp = shop.getIdPsp();
                if(ObjectUtils.anyNull(shop, shop.getIdPsp(), shop.getAbi(),
                        shop.getShopIdFirstPayment(), shop.getTerminalIdFirstPayment(), shop.getMacFirstPayment(),
                        shop.getShopIdSuccPayment(), shop.getTerminalIdSuccPayment(), shop.getMacSuccPayment())) {
                    log.error("Wrong shop number for idpsp: " + idPsp);
                } else {
                    vposShopMap.put(idPsp, shop);
                }
            }
        }
    }

    private VposShops getConfigShops(String vposShopsString) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(vposShopsString, VposShops.class);
    }

    public Shop getVposShopByIdPsp(String idPsp) {
        return vposShopMap.get(idPsp);
    }


}
