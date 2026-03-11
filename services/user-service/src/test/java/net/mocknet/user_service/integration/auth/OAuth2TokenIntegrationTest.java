package net.mocknet.user_service.integration.auth;

import net.mocknet.user_service.config.client.ClientsProperties;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static net.mocknet.user_service.common.security.PkceUtils.generateCodeChallengeS256;
import static net.mocknet.user_service.common.security.PkceUtils.generateCodeVerifier;

public class OAuth2TokenIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientsProperties clientsProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String responseType = "code";
    private String clientId;
    private String clientPassword;
    private String scope;
    private String redirectUri;
    private String codeVerifier;
    private String codeChallenge;
    private String codeChallengeMethod = "S256";
    private User activeUser;
    private final String password = "abcd1234";

    @BeforeEach
    void beforeEach() {
        ClientsProperties.ClientProperties clientProperties = this.clientsProperties.getClients().get("test-client");
        ClientsProperties.ClientProperties.RegistrationProperties registrationProperties = clientProperties.getRegistration();
        this.clientId = registrationProperties.getClientId();
        this.clientPassword = registrationProperties.getClientSecret();
        this.scope = String.join(" ", registrationProperties.getScopes());
        this.redirectUri = registrationProperties.getRedirectUris().get(0);

        this.codeVerifier = generateCodeVerifier();
        this.codeChallenge = generateCodeChallengeS256(codeVerifier);

        this.activeUser = createUser();
        this.activeUser.setId(null);
        this.activeUser.setPasswordHash(passwordEncoder.encode(password));
        this.activeUser = userRepository.save(activeUser);
    }

    @Test
    void tokenRequest_shouldExchangeCodeForTokens() {
        String jsessionid = oauth2AuthRequest()
            .returnResult()
            .getResponseCookies()
            .getFirst("JSESSIONID")
            .getValue();

        ExchangeResult loginResult = loginRequest(activeUser.getEmail(), password, jsessionid)
            .returnResult();

        String newJsessionid = loginResult.getResponseCookies().getFirst("JSESSIONID").getValue();
        URI redirectBackToAuthorize = loginResult.getResponseHeaders().getLocation();
        URI redirectLocation = restCli.get()
            .uri(redirectBackToAuthorize)
            .cookie("JSESSIONID", newJsessionid)
            .exchange().returnResult()
            .getResponseHeaders()
            .getLocation();

        String code = UriComponentsBuilder.fromUri(redirectLocation)
            .build()
            .getQueryParams()
            .getFirst("code");

        oauth2TokenRequest("authorization_code", code)
            .expectStatus().isOk()
            .expectBody(OAuth2AccessTokenResponse.class);
    }

    private RestTestClient.ResponseSpec oauth2AuthRequest() {
        return restCli.get()
            .uri(uriBuilder -> uriBuilder
                .path("/oauth2/authorize")
                .queryParam("response_type", responseType)
                .queryParam("client_id", clientId)
                .queryParam("scope", scope)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", codeChallengeMethod)
                .build()
            )
            .exchange();
    }

    private RestTestClient.ResponseSpec oauth2TokenRequest(String grantType, String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", grantType);
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);
        formData.add("code_verifier", codeVerifier);

        String authBasic = Base64.getEncoder().encodeToString(
            String.format("%s:%s", clientId, clientPassword).getBytes(StandardCharsets.UTF_8)
        );

        return restCli.post()
            .uri("/oauth2/token")
            .header("Authorization", "Basic " + authBasic)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange();
    }

    private RestTestClient.ResponseSpec loginRequest(String email, String password, String jsessionid) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", email);
        formData.add("password", password);

        return restCli.post()
            .uri("/login")
            .cookie("JSESSIONID", jsessionid)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange();
    }
}
