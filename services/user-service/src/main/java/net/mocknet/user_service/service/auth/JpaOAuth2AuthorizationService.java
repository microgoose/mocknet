package net.mocknet.user_service.service.auth;

import net.mocknet.user_service.model.client.Authorization;
import net.mocknet.user_service.repository.AuthorizationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final AuthorizationRepository authorizationRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationConverter authorizationConverter;

    public JpaOAuth2AuthorizationService(AuthorizationRepository authorizationRepository, RegisteredClientRepository registeredClientRepository) {
        this.authorizationRepository = authorizationRepository;
        this.registeredClientRepository = registeredClientRepository;
        ClassLoader classLoader = JpaOAuth2AuthorizationService.class.getClassLoader();
        this.authorizationConverter = new AuthorizationConverter(classLoader);
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        if (authorization == null)
            throw new IllegalArgumentException("Авторизация не может быть null");

        this.authorizationRepository.save(authorizationConverter.toEntity(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        if (authorization == null)
            throw new IllegalArgumentException("Авторизация не может быть null");

        this.authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("ID авторизации не может быть пустым");

        return this.authorizationRepository.findById(id)
            .map(this::toObject)
            .orElse(null);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Токен не может быть пустым");

        Optional<Authorization> result;
        if (tokenType == null) {
            result = this.authorizationRepository.findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(token);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByState(token);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByAuthorizationCodeValue(token);
        } else if (OAuth2ParameterNames.ACCESS_TOKEN.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByAccessTokenValue(token);
        } else if (OAuth2ParameterNames.REFRESH_TOKEN.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByRefreshTokenValue(token);
        } else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByOidcIdTokenValue(token);
        } else if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByUserCodeValue(token);
        } else if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
            result = this.authorizationRepository.findByDeviceCodeValue(token);
        } else {
            result = Optional.empty();
        }

        return result.map(this::toObject).orElse(null);
    }

    private OAuth2Authorization toObject(Authorization entity) {
        RegisteredClient registeredClient = this.registeredClientRepository.findById(entity.getRegisteredClientId());

        if (registeredClient == null)
            throw new IllegalStateException(
                "Клиент с ID «" + entity.getRegisteredClientId() + "» не найден"
            );

        return authorizationConverter.toObject(entity, registeredClient);
    }
}