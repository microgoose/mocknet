package net.mocknet.user_service.integration.auth;

import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;

class LoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private final String password = "abcd1234";

    @BeforeEach
    void setUp() {
        user = createUser();
        user.setId(null);
        user.setPasswordHash(passwordEncoder.encode(password));
        user = userRepository.save(user);
    }

    @Test
    void login_shouldSuccessLogin() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", user.getEmail());
        formData.add("password", password);

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectCookie().exists("JSESSIONID")
            .expectHeader().value("Location", location ->
                assertThat(location).doesNotContain("/login?error")
            );
    }

    @Test
    void login_LoginInsteadOfEmail_shouldFail() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", user.getLogin());
        formData.add("password", password);

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/login?error")
            );
    }

    @Test
    void login_NonExistentUser_shouldFail() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "some@user.com");
        formData.add("password", "wrongpassword");

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/login?error")
            );
    }

    @Test
    void login_withWrongPassword_shouldFail() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", user.getLogin());
        formData.add("password", "wrongpassword");

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/login?error")
            );
    }

    @Test
    void login_withInactiveUser_shouldFail() {
        User unactiveUser = createUser();
        unactiveUser.setId(null);
        unactiveUser.setActive(false);
        userRepository.save(unactiveUser);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", unactiveUser.getEmail());
        formData.add("password", password);

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/login?error")
            );
    }

    @Test
    void login_withUnverifiedUser_shouldFail() {
        User unverifiedUser = createUser();
        unverifiedUser.setId(null);
        unverifiedUser.setActive(false);
        userRepository.save(unverifiedUser);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", unverifiedUser.getEmail());
        formData.add("password", password);

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/login?error")
            );
    }

    @Test
    void login_withEmptyCredentials_shouldFail() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "");
        formData.add("password", "");

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/login?error")
            );
    }

    @Test
    void login_withGetMethod_shouldReturnLoginPage() {
        restCli.get()
            .uri("/login")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
            .expectBody()
            .consumeWith(result -> {
                String responseBody = new String(result.getResponseBody());
                assertThat(responseBody).contains("<form");
                assertThat(responseBody).contains("action=\"/login\"");
                assertThat(responseBody).contains("name=\"username\"");
                assertThat(responseBody).contains("name=\"password\"");
            });
    }

    @Test
    void login_shouldUpdateLastLoginTime() throws InterruptedException {
        OffsetDateTime beforeLogin = OffsetDateTime.now();
        Thread.sleep(100);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", user.getEmail());
        formData.add("password", password);

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound();

        User updatedUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isAfter(beforeLogin);
    }

    @Test
    void login_multipleFailedAttempts_shouldNotLockAccount() {
        for (int i = 0; i < 5; i++) {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("username", user.getEmail());
            formData.add("password", "wrongpassword" + i);

            restCli.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location ->
                    assertThat(location).contains("/login?error")
                );
        }

        User foundUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(foundUser.isActive()).isTrue();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", foundUser.getEmail());
        formData.add("password", password);

        restCli.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().value("Location", location ->
                assertThat(location).contains("/")
            );
    }
}