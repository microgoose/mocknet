package net.mocknet.user_service.integration.auth;

import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.EmailTokenRepository;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.Optional;

import static net.mocknet.user_service.common.factory.TestUserFactory.createRegisterRequest;
import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;

class RegisterUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    private User activeUser;

    @BeforeEach
    void setUp() {
        this.activeUser = createUser();
        this.activeUser.setId(null);
        this.activeUser.setVerified(true);
        this.activeUser = userRepository.save(activeUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        emailTokenRepository.deleteAll();
    }

    @Test
    void registerUser_ValidRequest_Success() {
        RegisterRequestDto validRequest = createRegisterRequest();

        RegisterResponseDto response = restCli.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RegisterResponseDto.class)
            .returnResult()
            .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getLogin()).isEqualTo(validRequest.getLogin());
        assertThat(response.getEmail()).isEqualTo(validRequest.getEmail());
        assertThat(response.getFirstName()).isEqualTo(validRequest.getFirstName());
        assertThat(response.getLastName()).isEqualTo(validRequest.getLastName());

        // Проверяем, что пользователь сохранился в БД
        Optional<User> savedUser = userRepository.findByEmail(validRequest.getEmail());
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().isVerified()).isFalse();

        // Проверяем, что создан хотя бы один токен верификации
        List<EmailToken> tokens = emailTokenRepository.findAllByUser(savedUser.get());
        assertThat(tokens).isNotEmpty();

        boolean hasUnusedToken = tokens.stream()
            .anyMatch(token -> token.getUsedAt() == null);
        assertThat(hasUnusedToken).isTrue();
    }

    @Test
    void registerUser_InvalidEmail_Failure() {
        RegisterRequestDto invalidRequest = createRegisterRequest();
        invalidRequest.setEmail("invalid-email");

        ProblemDetail problemDetail = restCli.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(invalidRequest)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(422);

        Optional<User> savedUser = userRepository.findByEmail(invalidRequest.getEmail());
        assertThat(savedUser).isEmpty();
    }

    @Test
    void registerUser_ExistingEmail_Conflict() {
        // Пытаемся зарегистрироваться с тем же email
        RegisterRequestDto secondRequest = createRegisterRequest();
        secondRequest.setEmail(activeUser.getEmail());
        secondRequest.setLogin("different_login");

        ProblemDetail problemDetail = restCli.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(secondRequest)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(409);

        // Проверяем, что второй пользователь не создан
        String existingEmail = activeUser.getEmail();
        List<User> usersWithEmail = userRepository.findAll().stream()
            .filter(user -> user.getEmail().equals(existingEmail))
            .toList();
        assertThat(usersWithEmail).hasSize(1);
    }

    @Test
    void registerUser_MissingFields_BadRequest() {
        // Чистим принудительно
        userRepository.deleteAll();

        RegisterRequestDto emptyRequest = new RegisterRequestDto();

        ProblemDetail problemDetail = restCli.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(emptyRequest)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(422);

        // Проверяем, что пользователи не создавались
        assertThat(userRepository.count()).isZero();
    }

}