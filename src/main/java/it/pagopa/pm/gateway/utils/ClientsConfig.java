package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("singleton")
@Slf4j
public class ClientsConfig {
    private final Map<String, ClientConfig> config;

    public ClientsConfig(@Autowired ObjectMapper mapper, @Value("${pgs.clients.config}") String config) throws JsonProcessingException {
        TypeReference<HashMap<String, ClientConfig>> mapType = new TypeReference<HashMap<String, ClientConfig>>() {};
        this.config = Collections.unmodifiableMap(mapper.readValue(config, mapType));
    }

    public ClientConfig getByKey(String key) {
        return config.get(key);
    }

    public boolean containsKey(String key) {
        return config.containsKey(key);
    }
}
