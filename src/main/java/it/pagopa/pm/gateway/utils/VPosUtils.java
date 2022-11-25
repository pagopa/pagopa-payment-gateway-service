package it.pagopa.pm.gateway.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String ASTERISK_SPLIT_CHAR = "\\*";
    private static final String PIPE_SPLIT_CHAR = "\\|";
    private final Map<String, List<String>> vposShopMap = new HashMap<>();

    @Value("${vpos.vposShops}")
    private String vposShops;

    // IDPSP|ABI|SHOP_ID_F|TERMINAL_ID_F|MAC_F|SHOP_ID_S|TERMINAL_ID_S|MAC_S*IDPSP|....
    @PostConstruct
    private void getVposShop() {
        List<String> allShops = getConfig(vposShops, ASTERISK_SPLIT_CHAR);
        for (String shop : allShops) {
            List<String> singleShop = getConfig(shop, PIPE_SPLIT_CHAR);
            vposShopMap.put(singleShop.get(0), singleShop);
        }
    }

    private List<String> getConfig(String config, String splitChar) {
        return Arrays.asList(config.split(splitChar));
    }

    public List<String> getVariables(String idPsp) {
        return vposShopMap.get(idPsp);
    }

}
