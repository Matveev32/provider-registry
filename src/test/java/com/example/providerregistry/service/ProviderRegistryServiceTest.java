package com.example.providerregistry.service;

import com.example.providerregistry.exception.ProviderNotFoundException;
import com.example.providerregistry.model.ProviderConfig;
import com.example.providerregistry.source.JsonFileProviderSource;
import com.example.providerregistry.source.ListProviderSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.example.providerregistry.service.ProviderRegistryService.API_KEY_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProviderRegistryServiceTest {

    private static final String PROVIDER_ID = "provider-1";
    private static final String BASE_URL = "https://api.provider1.com";
    private static final String API_KEY = "secret-key-1";
    private static final String PHONE = "79991234567";
    private static final String PHONE_URL = BASE_URL + "/subscribers/" + PHONE;
    private static final String OK_BODY = "{\"status\":\"ok\"}";

    private MockRestServiceServer mockServer;
    private ProviderRegistryService registryService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        registryService = new ProviderRegistryService(builder);
        registryService.setProviders(List.of(new ProviderConfig(PROVIDER_ID, BASE_URL, API_KEY)));
    }

    @Nested
    @DisplayName("Проверка доступности номера")
    class Availability {

        @Test
        @DisplayName("200 и {\"status\":\"ok\"} -> true, запрос GET с API-ключом")
        void returnsTrueOnOk() {
            mockServer.expect(requestTo(PHONE_URL))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(API_KEY_HEADER, API_KEY))
                    .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

            assertThat(registryService.isPhoneAvailable(PROVIDER_ID, PHONE)).isTrue();
            mockServer.verify();
        }

        @Test
        @DisplayName("Лишние поля в ответе игнорируются")
        void ignoresUnknownFields() {
            mockServer.expect(requestTo(PHONE_URL))
                    .andRespond(withSuccess("{\"status\":\"ok\",\"extra\":1}", MediaType.APPLICATION_JSON));

            assertThat(registryService.isPhoneAvailable(PROVIDER_ID, PHONE)).isTrue();
            mockServer.verify();
        }

        @Test
        @DisplayName("404 -> false")
        void returnsFalseOn404() {
            mockServer.expect(requestTo(PHONE_URL))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(API_KEY_HEADER, API_KEY))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThat(registryService.isPhoneAvailable(PROVIDER_ID, PHONE)).isFalse();
            mockServer.verify();
        }

        @ParameterizedTest
        @ValueSource(strings = {"{\"status\":\"fail\"}", "{}"})
        @DisplayName("200 с телом, где статус не ok -> false")
        void returnsFalseWhenStatusIsNotOk(String body) {
            mockServer.expect(requestTo(PHONE_URL))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            assertThat(registryService.isPhoneAvailable(PROVIDER_ID, PHONE)).isFalse();
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("Ошибки пробрасываются вызывающему коду")
    class ErrorPropagation {

        @ParameterizedTest
        @ValueSource(ints = {400, 401, 403, 429, 500, 502, 503})
        @DisplayName("HTTP-ошибки, кроме 404")
        void propagatesHttpErrors(int code) {
            mockServer.expect(requestTo(PHONE_URL))
                    .andRespond(withStatus(HttpStatus.valueOf(code)));

            assertThatThrownBy(() -> registryService.isPhoneAvailable(PROVIDER_ID, PHONE))
                    .isInstanceOfSatisfying(RestClientResponseException.class,
                            e -> assertThat(e.getStatusCode().value()).isEqualTo(code));
            mockServer.verify();
        }

        @Test
        @DisplayName("Сетевая ошибка -> ResourceAccessException")
        void propagatesNetworkError() {
            mockServer.expect(requestTo(PHONE_URL))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            assertThatThrownBy(() -> registryService.isPhoneAvailable(PROVIDER_ID, PHONE))
                    .isInstanceOf(ResourceAccessException.class)
                    .hasCauseInstanceOf(SocketTimeoutException.class);
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("Набор поставщиков")
    class Providers {

        @Test
        @DisplayName("Неизвестный поставщик -> ProviderNotFoundException")
        void unknownProvider() {
            assertThatThrownBy(() -> registryService.isPhoneAvailable("unknown", PHONE))
                    .isInstanceOf(ProviderNotFoundException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("Каждый поставщик ходит на свой URL со своим ключом")
        void routesToOwnProvider() {
            registryService.setProviders(List.of(
                    new ProviderConfig("a", "https://a.example.com", "key-a"),
                    new ProviderConfig("b", "https://b.example.com", "key-b")));

            mockServer.expect(requestTo("https://a.example.com/subscribers/" + PHONE))
                    .andExpect(header(API_KEY_HEADER, "key-a"))
                    .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));
            mockServer.expect(requestTo("https://b.example.com/subscribers/" + PHONE))
                    .andExpect(header(API_KEY_HEADER, "key-b"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThat(registryService.isPhoneAvailable("a", PHONE)).isTrue();
            assertThat(registryService.isPhoneAvailable("b", PHONE)).isFalse();
            mockServer.verify();
        }

        @Test
        @DisplayName("Замена набора целиком: старый удаляется, новый работает")
        void replacesWholeSet() {
            registryService.setProviders(List.of(
                    new ProviderConfig("new-provider", "https://new.example.com", "new-key")));

            assertThatThrownBy(() -> registryService.isPhoneAvailable(PROVIDER_ID, PHONE))
                    .isInstanceOf(ProviderNotFoundException.class);

            mockServer.expect(requestTo("https://new.example.com/subscribers/" + PHONE))
                    .andExpect(header(API_KEY_HEADER, "new-key"))
                    .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));
            assertThat(registryService.isPhoneAvailable("new-provider", PHONE)).isTrue();
            mockServer.verify();
        }

        @Test
        @DisplayName("Пустой список очищает реестр")
        void emptyListClearsRegistry() {
            registryService.setProviders(List.of());

            assertThatThrownBy(() -> registryService.isPhoneAvailable(PROVIDER_ID, PHONE))
                    .isInstanceOf(ProviderNotFoundException.class);
        }

        @Test
        @DisplayName("Дубликат id -> IllegalArgumentException, старый набор сохраняется")
        void duplicateIdKeepsOldSet() {
            ProviderConfig first = new ProviderConfig("dup", "https://x.example.com", "k1");
            ProviderConfig second = new ProviderConfig("dup", "https://y.example.com", "k2");

            assertThatThrownBy(() -> registryService.setProviders(List.of(first, second)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dup");

            mockServer.expect(requestTo(PHONE_URL))
                    .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));
            assertThat(registryService.isPhoneAvailable(PROVIDER_ID, PHONE)).isTrue();
        }

        @Test
        @DisplayName("null вместо коллекции или элемента -> NullPointerException")
        void rejectsNulls() {
            assertThatThrownBy(() -> registryService.setProviders(null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> registryService.setProviders(Collections.singletonList(null)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> registryService.replaceProviders(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Замена из готового списка (ListProviderSource)")
        void replaceFromList() {
            registryService.replaceProviders(new ListProviderSource(List.of(
                    new ProviderConfig("list-provider", "https://list.example.com", "list-key"))));

            mockServer.expect(requestTo("https://list.example.com/subscribers/" + PHONE))
                    .andExpect(header(API_KEY_HEADER, "list-key"))
                    .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));
            assertThat(registryService.isPhoneAvailable("list-provider", PHONE)).isTrue();
            mockServer.verify();
        }

        @Test
        @DisplayName("Замена из JSON-файла (JsonFileProviderSource)")
        void replaceFromJsonFile(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("providers.json");
            Files.writeString(file, """
                    [
                      {"id": "file-1", "baseUrl": "https://file1.example.com", "apiKey": "file-key-1"},
                      {"id": "file-2", "baseUrl": "https://file2.example.com", "apiKey": "file-key-2"}
                    ]
                    """);

            registryService.replaceProviders(new JsonFileProviderSource(file, new ObjectMapper()));

            mockServer.expect(requestTo("https://file2.example.com/subscribers/" + PHONE))
                    .andExpect(header(API_KEY_HEADER, "file-key-2"))
                    .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));
            assertThat(registryService.isPhoneAvailable("file-2", PHONE)).isTrue();
            assertThatThrownBy(() -> registryService.isPhoneAvailable(PROVIDER_ID, PHONE))
                    .isInstanceOf(ProviderNotFoundException.class);
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("Проверка аргументов")
    class Arguments {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void rejectsBlankProviderId(String providerId) {
            assertThatThrownBy(() -> registryService.isPhoneAvailable(providerId, PHONE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void rejectsBlankPhone(String phone) {
            assertThatThrownBy(() -> registryService.isPhoneAvailable(PROVIDER_ID, phone))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
