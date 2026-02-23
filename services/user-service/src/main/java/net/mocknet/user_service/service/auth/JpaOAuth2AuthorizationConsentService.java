package net.mocknet.user_service.service.auth;

import net.mocknet.user_service.model.client.AuthorizationConsent;
import net.mocknet.user_service.repository.AuthorizationConsentRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
public class JpaOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationConsentConverter authorizationConsentConverter;

    public JpaOAuth2AuthorizationConsentService(AuthorizationConsentRepository authorizationConsentRepository,
                                                RegisteredClientRepository registeredClientRepository,
                                                AuthorizationConsentConverter authorizationConsentConverter) {
        if (authorizationConsentRepository == null)
            throw new IllegalArgumentException("Репозиторий согласий не может быть null");
        if (registeredClientRepository == null)
            throw new IllegalArgumentException("Репозиторий клиентов не может быть null");
        if (authorizationConsentConverter == null)
            throw new IllegalArgumentException("Конвертер согласий не может быть null");

        this.authorizationConsentRepository = authorizationConsentRepository;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationConsentConverter = authorizationConsentConverter;
    }

    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        if (authorizationConsent == null)
            throw new IllegalArgumentException("Согласие авторизации не может быть null");

        this.authorizationConsentRepository.save(
            authorizationConsentConverter.toEntity(authorizationConsent)
        );
    }

    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        if (authorizationConsent == null)
            throw new IllegalArgumentException("Согласие авторизации не может быть null");

        this.authorizationConsentRepository.deleteByRegisteredClientIdAndPrincipalName(
            authorizationConsent.getRegisteredClientId(),
            authorizationConsent.getPrincipalName()
        );
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        if (registeredClientId == null || registeredClientId.isBlank())
            throw new IllegalArgumentException("ID клиента не может быть пустым");
        if (principalName == null || principalName.isBlank())
            throw new IllegalArgumentException("Имя принципала не может быть пустым");

        return this.authorizationConsentRepository
            .findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
            .map(this::toObject)
            .orElse(null);
    }

    private OAuth2AuthorizationConsent toObject(AuthorizationConsent authorizationConsent) {
        String registeredClientId = authorizationConsent.getRegisteredClientId();
        RegisteredClient registeredClient = this.registeredClientRepository.findById(registeredClientId);

        if (registeredClient == null)
            throw new IllegalStateException(
                "Клиент с ID «" + registeredClientId + "» не найден"
            );

        return authorizationConsentConverter.toObject(authorizationConsent, registeredClient);
    }
}