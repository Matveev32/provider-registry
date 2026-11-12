package com.example.providerregistry.service;


import com.example.providerregistry.exception.ProviderNotFoundException;
import com.example.providerregistry.model.ProviderConfig;
import com.example.providerregistry.model.SubscriberStatusResponse;
import com.example.providerregistry.source.ProviderSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ProviderRegistryService {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final RestClient.Builder restClientBuilder;
    private final AtomicReference<Map<String, RestClient>> clientsRef =
            new AtomicReference<>(Map.of());

    public ProviderRegistryService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = Objects.requireNonNull(restClientBuilder, "restClientBuilder must not be null");
    }

    public void setProviders(Collection<ProviderConfig> providers) {
        Objects.requireNonNull(providers, "providers must not be null");

        Map<String, RestClient> newClients = new HashMap<>();
        for (ProviderConfig config : providers) {
            Objects.requireNonNull(config, "provider config must not be null");
            if (newClients.containsKey(config.id())) {
                throw new IllegalArgumentException("Duplicate provider id: " + config.id());
            }
            newClients.put(config.id(), buildClient(config));
        }
        clientsRef.set(Map.copyOf(newClients));
    }

    public void replaceProviders(ProviderSource source) {
        Objects.requireNonNull(source, "source must not be null");
        setProviders(source.load());
    }

    public boolean isPhoneAvailable(String providerId, String phone) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }

        RestClient client = clientsRef.get().get(providerId);
        if (client == null) {
            throw new ProviderNotFoundException(providerId);
        }

        try {
            SubscriberStatusResponse response = client.get()
                    .uri("/subscribers/{phone}", phone)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SubscriberStatusResponse.class);
            return response != null && response.isAvailable();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    private RestClient buildClient(ProviderConfig config) {
        return restClientBuilder.clone()
                .baseUrl(config.baseUrl())
                .defaultHeader(API_KEY_HEADER, config.apiKey())
                .build();
    }
}