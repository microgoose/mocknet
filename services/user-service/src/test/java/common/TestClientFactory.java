package common;

import net.mocknet.user_service.model.client.Client;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

public final class TestClientFactory {

    public static final String ID = "client-123";
    public static final String CLIENT_ID = "test-client";
    public static final String CLIENT_SECRET = "secret";
    public static final String CLIENT_NAME = "Test Client";
    public static final Set<String> REDIRECT_URIS = Set.of("https://example.com/callback");
    public static final Set<AuthorizationGrantType> GRANT_TYPES = Set.of(
        AuthorizationGrantType.AUTHORIZATION_CODE,
        AuthorizationGrantType.REFRESH_TOKEN
    );
    public static final Set<String> SCOPES = Set.of("read", "write");
    public static final Instant CLIENT_ID_ISSUED_AT = Instant.now();
    public static final Instant CLIENT_SECRET_EXPIRES_AT = Instant.now().plus(365, ChronoUnit.DAYS);

    public static Client createClient() {
        Set<String> grantTypes = GRANT_TYPES.stream()
            .map(AuthorizationGrantType::getValue)
            .collect(Collectors.toSet());

        Client client = new Client();
        client.setId(ID);
        client.setClientId(CLIENT_ID);
        client.setClientSecret(CLIENT_SECRET);
        client.setClientName(CLIENT_NAME);
        client.setRedirectUris(String.join(",", REDIRECT_URIS));
        client.setAuthorizationGrantTypes(String.join(",", grantTypes));
        client.setScopes(String.join(",", SCOPES));

        return client;
    }

    public static RegisteredClient createRegisteredClient(String clientId, String clientName) {
        return RegisteredClient.withId(ID)
            .clientId(clientId)
            .clientSecret(CLIENT_SECRET)
            .clientName(clientName)
            .redirectUris(uris -> uris.addAll(REDIRECT_URIS))
            .authorizationGrantTypes(types -> types.addAll(GRANT_TYPES))
            .scopes(scopes -> scopes.addAll(SCOPES))
            .clientIdIssuedAt(CLIENT_ID_ISSUED_AT)
            .clientSecretExpiresAt(CLIENT_SECRET_EXPIRES_AT)
            .build();
    }

    public static RegisteredClient createRegisteredClient() {
        return createRegisteredClient(CLIENT_ID, CLIENT_NAME);
    }
}