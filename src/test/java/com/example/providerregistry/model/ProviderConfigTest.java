package com.example.providerregistry.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderConfigTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsBlankId(String value) {
        assertThatThrownBy(() -> new ProviderConfig(value, "https://a.example.com", "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsBlankBaseUrl(String value) {
        assertThatThrownBy(() -> new ProviderConfig("id", value, "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsBlankApiKey(String value) {
        assertThatThrownBy(() -> new ProviderConfig("id", "https://a.example.com", value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apiKey");
    }

    @Test
    void toStringDoesNotLeakApiKey() {
        ProviderConfig config = new ProviderConfig("p1", "https://a.example.com", "super-secret");

        assertThat(config.toString()).contains("p1").doesNotContain("super-secret");
    }
}
