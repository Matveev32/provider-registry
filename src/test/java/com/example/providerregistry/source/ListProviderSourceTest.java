package com.example.providerregistry.source;

import com.example.providerregistry.model.ProviderConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListProviderSourceTest {

    private final ProviderConfig config = new ProviderConfig("p1", "https://a.example.com", "key");

    @Test
    void returnsGivenProviders() {
        assertThat(new ListProviderSource(List.of(config)).load()).containsExactly(config);
    }

    @Test
    void makesDefensiveCopy() {
        List<ProviderConfig> original = new ArrayList<>(List.of(config));
        ListProviderSource source = new ListProviderSource(original);

        original.clear();

        assertThat(source.load()).containsExactly(config);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new ListProviderSource(null)).isInstanceOf(NullPointerException.class);
    }
}