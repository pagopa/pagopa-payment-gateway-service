package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.entity.PGSConfigEntity;
import it.pagopa.pm.gateway.repository.PGSConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class Config {

    @Autowired
    PGSConfigRepository pgsConfigRepository;

    private Map<String, String> configs = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("INIT Config START");
        try {
            refresh();
        } catch (Exception e) {
            log.warn("Exception", e);
        }
        log.info("INIT Config END");
    }

    public String getConfig(String key) {
        if (configs.containsKey(key)) {
            return configs.get(key);
        } else {
            // PM-512 this is for backwards compatibility in environments where the configs
            // are still defined as system properties in standalone.xml jboss file
            return System.getProperty(key);
        }
    }

    public void refresh() {
        List<PGSConfigEntity> resultList = pgsConfigRepository.findAll();
        synchronized (configs) {
            configs.clear();
            for (PGSConfigEntity config : resultList) {
                String key = config.getKey();
                if (StringUtils.isBlank(key)) {
                    throw new IllegalArgumentException("Cannot complete init process: found empty key");
                }
                log.debug("Loaded: " + key);
                String value = config.getValue();
                configs.put(key, value);
            }
        }
    }

}
