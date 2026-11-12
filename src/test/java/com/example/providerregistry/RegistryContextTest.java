package com.example.providerregistry;

import com.example.providerregistry.config.RegistryConfig;
import com.example.providerregistry.service.ProviderRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig({RegistryConfig.class, ProviderRegistryService.class})
class RegistryContextTest {

    @Autowired
    private ProviderRegistryService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextStartsAndBeansAreWired() {
        assertThat(service).isNotNull();
        assertThat(objectMapper).isNotNull();
    }
}
