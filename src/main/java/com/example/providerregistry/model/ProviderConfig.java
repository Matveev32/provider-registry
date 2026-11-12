package com.example.providerregistry.model;

public record ProviderConfig(String id, String baseUrl, String apiKey) {

    public ProviderConfig {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Provider id must not be blank");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Provider baseUrl must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Provider apiKey must not be blank");
        }
    }

    @Override
    public String toString() {
        return "ProviderConfig[id=" + id + ", baseUrl=" + baseUrl + ", apiKey=***]";
    }
}
