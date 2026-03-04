package net.mocknet.user_service.integration.auth;

import net.mocknet.user_service.config.client.ClientsProperties;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static net.mocknet.user_service.common.security.PkceUtils.generateCodeChallengeS256;
import static net.mocknet.user_service.common.security.PkceUtils.generateCodeVerifier;
import static org.assertj.core.api.Assertions.assertThat;

public class OAuth2AuthorizeIntegrationTest  extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientsProperties clientsProperties;

    private String responseType = "code";
    private String clientId;
    private String scope;
    private String redirectUri;
    private String codeVerifier;
    private String codeChallenge;
    private String codeChallengeMethod = "S256";
    private User activeUser;

    @BeforeEach
    void beforeEach() {
        ClientsProperties.ClientProperties clientProperties = this.clientsProperties.getClients().get("test-client");
        ClientsProperties.ClientProperties.RegistrationProperties registrationProperties = clientProperties.getRegistration();
        this.clientId = registrationProperties.getClientId();
        this.scope = String.join(" ", registrationProperties.getScopes());
        this.redirectUri = registrationProperties.getRedirectUris().get(0);

        this.codeVerifier = generateCodeVerifier();
        this.codeChallenge = generateCodeChallengeS256(codeVerifier);

        this.activeUser = createUser();
        this.activeUser.setId(null);
        this.activeUser = userRepository.save(activeUser);
    }

    @Test
    void authorizeRequest_shouldRedirectToLogin() {
        RestTestClient.ResponseSpec response = authRequest();
        response.expectStatus().is3xxRedirection();
        String location = response.returnResult()
            .getResponseHeaders()
            .getLocation()
            .toString();

        assertThat(location).contains("/login");
    }

    private RestTestClient.ResponseSpec authRequest() {
        return restCli.get()
            .uri("/oauth2/authorize", Map.of(
                "response_type", responseType,
                "client_id", clientId,
                "scope", scope,
                "redirect_uri", redirectUri,
                "code_challenge", codeChallenge,
                "code_challenge_method", codeChallengeMethod
            ))
            .exchange();
    }
}
