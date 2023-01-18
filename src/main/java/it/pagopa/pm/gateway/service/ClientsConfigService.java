package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.ClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@Scope("singleton")
public class ClientsConfigService {
    @Value("${pgs.clients.config}")
    private String clientsConfigJson;
    private Map<String, ClientConfig> clientsConfigMap;

    @PostConstruct
    private void convertJsonToMap() throws JsonProcessingException {
        TypeReference<HashMap<String, ClientConfig>> mapType = new TypeReference<HashMap<String, ClientConfig>>() {};
        clientsConfigMap = new ObjectMapper().readValue(clientsConfigJson, mapType);
    }

    public ClientConfig getByKey(String key) {
        return clientsConfigMap.get(key);
    }

    public boolean containsKey(String key) {
        return clientsConfigMap.containsKey(key);
    }
}
