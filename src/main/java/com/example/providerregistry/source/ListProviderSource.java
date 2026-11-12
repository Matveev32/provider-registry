package com.example.providerregistry.source;

import com.example.providerregistry.model.ProviderConfig;
import java.util.List;

public record ListProviderSource(List<ProviderConfig> providers) implements ProviderSource {

    public ListProviderSource {
        providers = List.copyOf(providers);
    }

    @Override
    public List<ProviderConfig> load() {
        return providers;
    }
}
