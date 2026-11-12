package com.example.providerregistry.source;

import com.example.providerregistry.model.ProviderConfig;

import java.util.List;

public interface ProviderSource {
    List<ProviderConfig> load();
}