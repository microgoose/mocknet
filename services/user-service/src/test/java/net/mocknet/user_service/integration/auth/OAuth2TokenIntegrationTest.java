package net.mocknet.user_service.integration.auth;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;

import net.mocknet.user_service.common.client.OAuth2TestClient;
import net.mocknet.user_service.config.client.ClientsProperties;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

public class OAuth2TokenIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientsProperties clientsProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OAuth2TestClient oauth2Client;
    private User activeUser;
    private final String password = "abcd1234";

    @BeforeEach
    void beforeEach() {
        this.oauth2Client = OAuth2TestClient.forClient(
            restCli,
            clientsProperties,
            "test-client"
        );

        this.activeUser = createUser();
        this.activeUser.setId(null);
        this.activeUser.setPasswordHash(passwordEncoder.encode(password));
        this.activeUser = userRepository.save(activeUser);
    }

    @Test
    void tokenRequest_shouldExchangeCodeForTokens() {
        String code = oauth2Client.obtainAuthorizationCode(
            activeUser,
            password
        );

        oauth2Client
            .exchangeCode(code)
            .expectStatus()
            .isOk()
            .expectBody(OAuth2AccessTokenResponse.class);
    }

    @Test
    void tokenRequest_invalidCode_shouldReturnBadRequest() {
        oauth2Client
            .exchangeCode("invalid-code-1234")
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void tokenRequest_secondRequestWithSameCode_shouldReturnBadRequest() {
        String code = oauth2Client.obtainAuthorizationCode(
            activeUser,
            password
        );

        oauth2Client.exchangeCode(code).expectStatus().isOk();
        oauth2Client.exchangeCode(code).expectStatus().isBadRequest();
    }

    @Test
    void tokenRequest_missingClientAuth_shouldReturnUnauthorized() {
        String code = oauth2Client.obtainAuthorizationCode(
            activeUser,
            password
        );

        oauth2Client
            .exchangeCodeWithBasicAuth(code, "non-existent-client", "secret")
            .expectStatus()
            .isUnauthorized();
    }
}
