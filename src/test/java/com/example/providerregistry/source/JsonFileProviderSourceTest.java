package com.example.providerregistry.source;

import com.example.providerregistry.model.ProviderConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFileProviderSourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private JsonFileProviderSource sourceWith(String json) throws IOException {
        Path file = tempDir.resolve("providers.json");
        Files.writeString(file, json);
        return new JsonFileProviderSource(file, objectMapper);
    }

    @Test
    void readsProviders() throws IOException {
        var source = sourceWith("""
                [
                  {"id": "p1", "baseUrl": "https://a.example.com", "apiKey": "k1"},
                  {"id": "p2", "baseUrl": "https://b.example.com", "apiKey": "k2"}
                ]
                """);

        assertThat(source.load()).containsExactly(
                new ProviderConfig("p1", "https://a.example.com", "k1"),
                new ProviderConfig("p2", "https://b.example.com", "k2"));
    }

    @Test
    void emptyArrayGivesEmptyList() throws IOException {
        assertThat(sourceWith("[]").load()).isEmpty();
    }

    @Test
    void missingFileThrowsUncheckedIOException() {
        var source = new JsonFileProviderSource(tempDir.resolve("nope.json"), objectMapper);

        assertThatThrownBy(source::load).isInstanceOf(UncheckedIOException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not a json",
            "{\"id\":\"p1\"}",
            "[{\"id\":\"p1\",\"baseUrl\":\"https://a.example.com\"}]",
            "[{\"id\":\"p1\",\"baseUrl\":\"https://a.example.com\",\"apiKey\":\" \"}]"
    })
    void invalidContentThrowsUncheckedIOException(String json) throws IOException {
        var source = sourceWith(json);

        assertThatThrownBy(source::load).isInstanceOf(UncheckedIOException.class);
    }
}
