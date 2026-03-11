package net.mocknet.user_service.common.client;

import static net.mocknet.user_service.common.security.PkceUtils.generateCodeChallengeS256;
import static net.mocknet.user_service.common.security.PkceUtils.generateCodeVerifier;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.Getter;
import net.mocknet.user_service.config.client.ClientsProperties;
import net.mocknet.user_service.model.user.User;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
public class OAuth2TestClient {

    private final RestTestClient restCli;
    private String clientId;
    private final String clientSecret;
    private final String scope;
    private String redirectUri;

    private String responseType = "code";
    private String codeVerifier = generateCodeVerifier();
    private String codeChallenge = generateCodeChallengeS256(codeVerifier);
    private String codeChallengeMethod = "S256";

    public OAuth2TestClient(
        RestTestClient restCli,
        ClientsProperties clientsProperties,
        String clientKey
    ) {
        this.restCli = restCli;

        ClientsProperties.ClientProperties clientProperties = clientsProperties
            .getClients()
            .get(clientKey);
        ClientsProperties.ClientProperties.RegistrationProperties registration =
            clientProperties.getRegistration();

        this.clientId = registration.getClientId();
        this.clientSecret = registration.getClientSecret();
        this.scope = String.join(" ", registration.getScopes());
        this.redirectUri = registration.getRedirectUris().get(0);
    }

    public static OAuth2TestClient forClient(
        RestTestClient restCli,
        ClientsProperties clientsProperties,
        String clientKey
    ) {
        return new OAuth2TestClient(restCli, clientsProperties, clientKey);
    }

    public OAuth2TestClient withResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public OAuth2TestClient withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuth2TestClient withRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public OAuth2TestClient withPkce(
        String codeVerifier,
        String codeChallenge,
        String codeChallengeMethod
    ) {
        this.codeVerifier = codeVerifier;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
        return this;
    }

    public OAuth2TestClient regeneratePkce() {
        this.codeVerifier = generateCodeVerifier();
        this.codeChallenge = generateCodeChallengeS256(codeVerifier);
        this.codeChallengeMethod = "S256";
        return this;
    }

    public RestTestClient.ResponseSpec authorize() {
        return restCli
            .get()
            .uri(uriBuilder ->
                uriBuilder
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

    public RestTestClient.ResponseSpec login(
        User user,
        String password,
        String jsessionId
    ) {
        return login(user.getEmail(), password, jsessionId);
    }

    public RestTestClient.ResponseSpec login(
        String email,
        String password,
        String jsessionId
    ) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", email);
        formData.add("password", password);

        return restCli
            .post()
            .uri("/login")
            .cookie("JSESSIONID", jsessionId)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange();
    }

    public String obtainAuthorizationCode(User user, String password) {
        String jsessionId = authorize()
            .returnResult()
            .getResponseCookies()
            .getFirst("JSESSIONID")
            .getValue();

        ExchangeResult loginResult = login(
            user,
            password,
            jsessionId
        ).returnResult();
        String authenticatedSessionId = loginResult
            .getResponseCookies()
            .getFirst("JSESSIONID")
            .getValue();
        URI redirectBackToAuthorize = loginResult
            .getResponseHeaders()
            .getLocation();

        URI redirectLocation = restCli
            .get()
            .uri(redirectBackToAuthorize)
            .cookie("JSESSIONID", authenticatedSessionId)
            .exchange()
            .returnResult()
            .getResponseHeaders()
            .getLocation();

        return UriComponentsBuilder.fromUri(redirectLocation)
            .build()
            .getQueryParams()
            .getFirst("code");
    }

    public RestTestClient.ResponseSpec exchangeCode(String code) {
        return exchangeCodeWithBasicAuth(code, clientId, clientSecret);
    }

    public RestTestClient.ResponseSpec exchangeCodeWithBasicAuth(
        String code,
        String clientId,
        String clientSecret
    ) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);
        formData.add("code_verifier", codeVerifier);

        return restCli
            .post()
            .uri("/oauth2/token")
            .header(
                "Authorization",
                "Basic " + basicAuth(clientId, clientSecret)
            )
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange();
    }

    public RestTestClient.ResponseSpec exchangeCodeWithoutClientAuthentication(
        String code
    ) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);
        formData.add("code_verifier", codeVerifier);

        return restCli
            .post()
            .uri("/oauth2/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange();
    }

    private String basicAuth(String clientId, String clientSecret) {
        return Base64.getEncoder().encodeToString(
            String.format("%s:%s", clientId, clientSecret).getBytes(
                StandardCharsets.UTF_8
            )
        );
    }
}
