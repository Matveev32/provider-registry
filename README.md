# provider-registry

Сервис-реестр HTTP-клиентов поставщиков. По идентификатору поставщика и номеру
телефона определяет, доступен ли номер.

## Стек
Java 17, Spring (spring-web `RestClient`, без веб-сервера), Jackson, JUnit 5,
AssertJ, `MockRestServiceServer`. Сборка: Maven.

## Поведение
Запрос: `GET {baseUrl}/subscribers/{phone}` с заголовком `X-Api-Key`.

| Ответ поставщика | Результат |
|---|---|
| 200 и `{"status":"ok"}` | `true`, номер доступен |
| 404 | `false`, номер недоступен |
| 200 с другим телом | `false` |
| любые другие HTTP-ошибки | исключение (`RestClientResponseException`) |
| сетевые ошибки | исключение (`ResourceAccessException`) |
| неизвестный поставщик | `ProviderNotFoundException` |

## Поставщики
Поставщик: `id`, `baseUrl`, `apiKey`. Набор заменяется целиком и атомарно
(`ProviderRegistryService#setProviders` / `#replaceProviders`).
При ошибке валидации (дубликат id, пустые поля) прежний набор сохраняется.

Источники (`ProviderSource`):
- `ListProviderSource` — готовый список;
- `JsonFileProviderSource` — JSON-файл на диске:

  [{"id": "p1", "baseUrl": "https://api.example.com", "apiKey": "secret"}]

## Решения
- Для каждого поставщика создаётся свой `RestClient` с `baseUrl` и ключом.
- Состояние реестра — неизменяемая `Map` в `AtomicReference`: замена без
  блокировок, параллельные запросы видят старый или новый набор целиком.
- `apiKey` не попадает в `toString()` конфигурации.
- Таймауты соединения и чтения заданы в `RegistryConfig`.
- Повторные попытки, OAuth, база и веб-приложение по условию не используются.

## Тесты
Реальный HTTP не используется: сеть подменяется `MockRestServiceServer`.

    