package com.example.providerregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriberStatusResponse(String status) {

    private static final String OK_STATUS = "ok";

    @JsonIgnore
    public boolean isAvailable() {
        return OK_STATUS.equals(status);
    }
}
