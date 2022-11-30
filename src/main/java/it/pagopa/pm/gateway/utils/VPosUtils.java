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

import static it.pagopa.pm.gateway.constant.VposConstant.ID_PSP_POSITION;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class VPosUtils {

    private static final String ASTERISK_SPLIT_CHAR = "\\*";
    private static final String PIPE_SPLIT_CHAR = "\\|";
    private final Map<String, List<String>> vposShopMap = new HashMap<>();

    @Value("${vpos.vposShops}")
    private String vposShops;

    /*
     * A VPos shop is identified by the following parameters:
     * idPsp, ABI, shopId, terminalId, MAC
     * where shopId, terminalId and MAC are different for first (F) or subsequent (S) payments.
     * Each shop property in the config environment variable is separated by a | and each shop is separated by a *
     * following this schema:
     * idPsp|ABI|shopId(F)|terminalId(F)|MAC(F)|shopId(S)|terminalId(S)|MAC(S)*idPsp|... ...terminalId(S)|mac(S)
     */
    @PostConstruct
    public void getVposShop() {
        List<String> allShops = getConfig(vposShops, ASTERISK_SPLIT_CHAR);
        for (String shop : allShops) {
            List<String> singleShop = getConfig(shop, PIPE_SPLIT_CHAR);
            vposShopMap.put(singleShop.get(ID_PSP_POSITION), singleShop);
        }
    }

    private List<String> getConfig(String config, String splitChar) {
        return Arrays.asList(config.split(splitChar));
    }

    public List<String> getVposShopByIdPsp(String idPsp) {
        return vposShopMap.get(idPsp);
    }

}
