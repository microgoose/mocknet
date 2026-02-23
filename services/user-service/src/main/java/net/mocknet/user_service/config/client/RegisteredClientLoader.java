package net.mocknet.user_service.config.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisteredClientLoader implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;
    private final ClientsProperties clientsProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        clientsProperties.getClients().forEach((name, props) -> {
            ClientsProperties.ClientProperties.RegistrationProperties reg = props.getRegistration();

            if (registeredClientRepository.findByClientId(reg.getClientId()) != null) {
                log.info("Client '{}' already registered, skipping", reg.getClientId());
                return;
            }

            RegisteredClient.Builder builder = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(reg.getClientId())
                .clientSecret(passwordEncoder.encode(reg.getClientSecret()))
                .clientIdIssuedAt(Instant.now());

            reg.getClientAuthenticationMethods().forEach(method ->
                builder.clientAuthenticationMethod(
                    new ClientAuthenticationMethod(method)
                )
            );

            reg.getAuthorizationGrantTypes().forEach(grantType ->
                builder.authorizationGrantType(
                    new AuthorizationGrantType(grantType)
                )
            );

            reg.getRedirectUris().forEach(builder::redirectUri);
            reg.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
            reg.getScopes().forEach(builder::scope);

            builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(props.isRequireAuthorizationConsent())
                .requireProofKey(props.isRequireProofKey())
                .build()
            );

            RegisteredClient client = builder.build();
            registeredClientRepository.save(client);
            log.info("Client '{}' successfully registered", reg.getClientId());
        });
    }
}