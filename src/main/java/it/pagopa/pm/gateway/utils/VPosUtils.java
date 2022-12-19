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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class VPosUtils {

    private final Map<String, Shop> vposShopMap = new HashMap<>();
    private static final String REQ_REF_NUM_FORMAT = "yyyyMMddHHmmssSSS";

    @Autowired
    private Environment environment;

    @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    public void getVposShop() throws JsonProcessingException {
        String vposShopsString = environment.getProperty("vpos.vposShops");
        if (vposShopsString != null) {
            VposShops configShops = getConfigShops(vposShopsString);
            List<Shop> allShops = configShops.getShops();
            for (Shop shop : allShops) {
                String idPsp = shop.getIdPsp();
                if (ObjectUtils.anyNull(shop, shop.getIdPsp(), shop.getAbi(),
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
        return objectMapper.readValue(vposShopsString, VposShops.class);
    }

    public Shop getVposShopByIdPsp(String idPsp) {
        return vposShopMap.get(idPsp);
    }

    public String getReqRefNum() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(REQ_REF_NUM_FORMAT);
        String guid = String.format("%040d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
        String reqRefNum = dateFormat.format(date) + guid;
        return reqRefNum.substring(0, 32);
    }

}
