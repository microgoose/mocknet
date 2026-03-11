package net.mocknet.user_service.integration.auth;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;

import net.mocknet.user_service.common.client.OAuth2TestClient;
import net.mocknet.user_service.config.client.ClientsProperties;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

public class OAuth2AuthorizeIntegrationTest extends AbstractIntegrationTest {

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
    void authorizeRequest_shouldRedirectToLogin() {
        oauth2Client
            .authorize()
            .expectStatus()
            .is3xxRedirection()
            .expectHeader()
            .value("Location", location -> {
                assertThat(location).contains("/login");
                assertThat(location).doesNotContain("error");
            });
    }

    @Test
    void authorizeRequest_invalidClientId_shouldReturnBadRequest() {
        OAuth2TestClient.forClient(restCli, clientsProperties, "test-client")
            .withClientId("non-existent-client-1234")
            .authorize()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void authorizeRequest_missingClientId_shouldReturnBadRequest() {
        OAuth2TestClient.forClient(restCli, clientsProperties, "test-client")
            .withClientId("")
            .authorize()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void authorizeRequest_invalidRedirectUri_shouldReturnBadRequest() {
        oauth2Client
            .withRedirectUri("non-existent-redirect-uri-1234")
            .authorize()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void authorizeRequest_denyResponseType_shouldReturnBadRequest() {
        oauth2Client
            .withResponseType("token")
            .authorize()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void authorizeRequest_shouldRedirectToRedirectUriWithCode() {
        String jsessionid = oauth2Client
            .authorize()
            .returnResult()
            .getResponseCookies()
            .getFirst("JSESSIONID")
            .getValue();

        var loginResult = oauth2Client
            .login(activeUser.getEmail(), password, jsessionid)
            .returnResult();

        String newJsessionid = loginResult
            .getResponseCookies()
            .getFirst("JSESSIONID")
            .getValue();
        var redirectBackToAuthorize = loginResult
            .getResponseHeaders()
            .getLocation();

        restCli
            .get()
            .uri(redirectBackToAuthorize)
            .cookie("JSESSIONID", newJsessionid)
            .exchange()
            .expectStatus()
            .is3xxRedirection()
            .expectHeader()
            .value("Location", location -> {
                assertThat(location).startsWith(oauth2Client.getRedirectUri());
                assertThat(location).contains("code=");
            });
    }
}
