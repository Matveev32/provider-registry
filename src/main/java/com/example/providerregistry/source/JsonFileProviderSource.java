package com.example.providerregistry.source;

import com.example.providerregistry.model.ProviderConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class JsonFileProviderSource implements ProviderSource {

    private final Path file;
    private final ObjectMapper objectMapper;

    public JsonFileProviderSource(Path file, ObjectMapper objectMapper) {
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public List<ProviderConfig> load() {
        try {
            return objectMapper.readValue(file.toFile(), new TypeReference<List<ProviderConfig>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read providers from JSON file: " + file, e);
        }
    }
}
