package net.mocknet.user_service.integration.user;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.EmailTokenRepository;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;

class GetProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    private User existingUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        this.existingUser = createUser();
        this.existingUser.setId(null);
        this.existingUser.setVerified(true);
        this.existingUser = userRepository.save(existingUser);

        this.userToken = tokenGenerator.generateBearerToken(this.existingUser);
    }

    @AfterEach
    void tearDown() {
        emailTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getProfile_ShouldReturnProfile() {
        ProfileDto response = restCli.get()
            .uri("/api/v1/user/{id}", existingUser.getId())
            .header("Authorization", "Bearer " + userToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ProfileDto.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(existingUser.getId());
        assertThat(response.getLogin()).isEqualTo(existingUser.getLogin());
        assertThat(response.getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(response.getFirstName()).isEqualTo(existingUser.getFirstName());
        assertThat(response.getLastName()).isEqualTo(existingUser.getLastName());
        assertThat(response.getAvatarUrl()).isEqualTo(existingUser.getAvatarUrl());
    }

    @Test
    void getProfile_WhenNonExist_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ProblemDetail problemDetail = restCli.get()
            .uri("/api/v1/user/{id}", nonExistentId)
            .header("Authorization", "Bearer " + userToken)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(404);
    }

}