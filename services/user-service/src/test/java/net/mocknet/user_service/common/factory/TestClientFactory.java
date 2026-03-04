package net.mocknet.user_service.common.factory;

import net.mocknet.user_service.model.client.Client;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Компонент для создания уникальных тестовых клиентов.
 */
public final class TestClientFactory {
    
    // Базовые значения для генерации уникальных данных
    private static final String BASE_CLIENT_ID = "test-client";
    private static final String BASE_CLIENT_SECRET = "secret";
    private static final String BASE_CLIENT_NAME = "Test Client";
    private static final Set<String> BASE_REDIRECT_URIS = Set.of("https://example.com/callback");
    private static final Set<AuthorizationGrantType> BASE_GRANT_TYPES = Set.of(
        AuthorizationGrantType.AUTHORIZATION_CODE,
        AuthorizationGrantType.REFRESH_TOKEN
    );
    private static final Set<String> BASE_SCOPES = Set.of("read", "write");
    private static final int CLIENT_SECRET_VALIDITY_DAYS = 365;

    private TestClientFactory() {
        // Приватный конструктор для предотвращения инстанциирования
    }

    /**
     * Создает нового клиента с уникальными данными
     */
    public static Client createClient() {
        String uniqueId = UUID.randomUUID().toString();
        String uniqueClientId = String.format("%s-%s", BASE_CLIENT_ID, 
            uniqueId.substring(0, 8));
        String uniqueClientName = String.format("%s-%s", BASE_CLIENT_NAME, 
            uniqueId.substring(0, 8));
        
        Set<String> grantTypes = BASE_GRANT_TYPES.stream()
            .map(AuthorizationGrantType::getValue)
            .collect(Collectors.toSet());

        Client client = new Client();
        client.setId(uniqueId);
        client.setClientId(uniqueClientId);
        client.setClientSecret(BASE_CLIENT_SECRET);
        client.setClientName(uniqueClientName);
        client.setRedirectUris(String.join(",", BASE_REDIRECT_URIS));
        client.setAuthorizationGrantTypes(String.join(",", grantTypes));
        client.setScopes(String.join(",", BASE_SCOPES));

        return client;
    }

    /**
     * Создает нового клиента с указанным ID
     */
    public static Client createClient(String customClientId) {
        String uniqueId = UUID.randomUUID().toString();
        String uniqueClientName = String.format("%s-%s", BASE_CLIENT_NAME, 
            uniqueId.substring(0, 8));
        
        Set<String> grantTypes = BASE_GRANT_TYPES.stream()
            .map(AuthorizationGrantType::getValue)
            .collect(Collectors.toSet());

        Client client = new Client();
        client.setId(uniqueId);
        client.setClientId(customClientId);
        client.setClientSecret(BASE_CLIENT_SECRET);
        client.setClientName(uniqueClientName);
        client.setRedirectUris(String.join(",", BASE_REDIRECT_URIS));
        client.setAuthorizationGrantTypes(String.join(",", grantTypes));
        client.setScopes(String.join(",", BASE_SCOPES));

        return client;
    }

    /**
     * Создает нового RegisteredClient с уникальными данными
     */
    public static RegisteredClient createRegisteredClient(String id, String clientId, String clientName) {
        return RegisteredClient.withId(id)
            .clientId(clientId)
            .clientSecret(BASE_CLIENT_SECRET)
            .clientName(clientName)
            .redirectUris(uris -> uris.addAll(BASE_REDIRECT_URIS))
            .authorizationGrantTypes(types -> types.addAll(BASE_GRANT_TYPES))
            .scopes(scopes -> scopes.addAll(BASE_SCOPES))
            .clientIdIssuedAt(Instant.now())
            .clientSecretExpiresAt(Instant.now().plus(CLIENT_SECRET_VALIDITY_DAYS, ChronoUnit.DAYS))
            .build();
    }

    /**
     * Создает нового RegisteredClient с уникальными данными
     */
    public static RegisteredClient createRegisteredClient() {
        String uniqueId = UUID.randomUUID().toString();
        String uniqueClientId = String.format("%s-%s", BASE_CLIENT_ID, 
            uniqueId.substring(0, 8));
        String uniqueClientName = String.format("%s-%s", BASE_CLIENT_NAME, 
            uniqueId.substring(0, 8));
        
        return createRegisteredClient(uniqueId, uniqueClientId, uniqueClientName);
    }
}