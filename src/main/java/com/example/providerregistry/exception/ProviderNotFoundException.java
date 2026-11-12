package com.example.providerregistry.exception;

public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String providerId) {
        super("Provider with id '" + providerId + "' is not registered");
    }
}
