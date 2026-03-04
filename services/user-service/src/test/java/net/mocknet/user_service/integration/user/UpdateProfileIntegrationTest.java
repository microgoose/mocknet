package net.mocknet.user_service.integration.user;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.UpdateProfileRequest;
import net.mocknet.user_service.integration.AbstractIntegrationTest;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.EmailTokenRepository;
import net.mocknet.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;
import java.util.UUID;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUpdateProfileRequest;
import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    private User existingUser;
    private String existingUserToken;
    private User anotherUser;
    private String anotherUserToken;

    @BeforeEach
    void setUp() {
        this.existingUser = createUser();
        this.existingUser.setId(null);
        this.existingUser.setVerified(true);
        this.existingUser = userRepository.save(existingUser);
        this.existingUserToken = tokenGenerator.generateBearerToken(existingUser);

        this.anotherUser = createUser();
        this.anotherUser.setId(null);
        this.anotherUser.setLogin("another_user");
        this.anotherUser.setEmail("another@example.com");
        this.anotherUser.setVerified(true);
        this.anotherUser = userRepository.save(anotherUser);
        this.anotherUserToken = tokenGenerator.generateBearerToken(anotherUser);
    }

    @AfterEach
    void tearDown() {
        emailTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void updateProfile_WhenNonExist_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        anotherUser.setId(nonExistentId);
        anotherUserToken = tokenGenerator.generateBearerToken(anotherUser);
        UpdateProfileRequest updateRequest = createUpdateProfileRequest();

        ProblemDetail problemDetail = updateProfileRequest(nonExistentId, anotherUserToken, updateRequest)
            .expectStatus().isNotFound()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(404);
    }

    @Test
    void updateProfile_WhenLoginExist_ShouldReturnConflict() {
        UpdateProfileRequest updateRequest = createUpdateProfileRequest();
        updateRequest.setLogin(anotherUser.getLogin());
        updateRequest.setEmail(existingUser.getEmail());

        ProblemDetail problemDetail = updateProfileRequest(existingUser.getId(), existingUserToken, updateRequest)
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(409);

        Optional<User> unchangedUser = userRepository.findById(existingUser.getId());
        assertThat(unchangedUser).isPresent();
        assertThat(unchangedUser.get().getLogin()).isEqualTo(existingUser.getLogin());
    }

    @Test
    void updateProfile_WhenEmailExist_ShouldReturnConflict() {
        UpdateProfileRequest updateRequest = createUpdateProfileRequest();
        updateRequest.setEmail(anotherUser.getEmail());
        updateRequest.setLogin(existingUser.getLogin());

        ProblemDetail problemDetail = updateProfileRequest(existingUser.getId(), existingUserToken, updateRequest)
            .expectStatus().isEqualTo(409)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(409);

        Optional<User> unchangedUser = userRepository.findById(existingUser.getId());
        assertThat(unchangedUser).isPresent();
        assertThat(unchangedUser.get().getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(unchangedUser.get().isVerified()).isTrue();
    }

    @Test
    void updateProfile_ForSomeoneElse_ShouldReturnForbidden() {
        // Пытаемся обновить профиль другого пользователя, будучи аутентифицированным как existingUser
        UpdateProfileRequest updateRequest = createUpdateProfileRequest();

        ProblemDetail problemDetail = updateProfileRequest(anotherUser.getId(), existingUserToken, updateRequest)
            .expectStatus().isForbidden()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(403);
    }

    @Test
    void updateProfile_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        UpdateProfileRequest partialUpdate = new UpdateProfileRequest();
        partialUpdate.setFirstName("Updated First");
        partialUpdate.setLastName("Updated Last");

        ProfileDto response = updateProfileRequest(existingUser.getId(), existingUserToken, partialUpdate)
            .expectStatus().isOk()
            .expectBody(ProfileDto.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("Updated First");
        assertThat(response.getLastName()).isEqualTo("Updated Last");
        assertThat(response.getLogin()).isEqualTo(existingUser.getLogin());
        assertThat(response.getEmail()).isEqualTo(existingUser.getEmail());
    }

    @Test
    void updateProfile_InvalidData_ShouldReturnValidationError() {
        UpdateProfileRequest invalidRequest = new UpdateProfileRequest();
        invalidRequest.setLogin("a");
        invalidRequest.setEmail("invalid-email");

        ProblemDetail problemDetail = updateProfileRequest(existingUser.getId(), existingUserToken, invalidRequest)
            .expectStatus().isEqualTo(422)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(422);
    }

    @Test
    void updateProfile_SameLoginAndEmail_ShouldNotCreateVerificationToken() {
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setLogin(existingUser.getLogin());
        updateRequest.setEmail(existingUser.getEmail());
        updateRequest.setFirstName("New First Name");

        ProfileDto response = updateProfileRequest(existingUser.getId(), existingUserToken, updateRequest)
            .expectStatus().isOk()
            .expectBody(ProfileDto.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("New First Name");

        Optional<User> updatedUser = userRepository.findById(existingUser.getId());
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().isVerified()).isTrue();
        assertThat(emailTokenRepository.findAllByUser(updatedUser.get())).isEmpty();
    }

    @Test
    void updateProfile_InvalidAvatarUrl_ShouldReturnValidationError() {
        UpdateProfileRequest invalidRequest = new UpdateProfileRequest();
        invalidRequest.setAvatarUrl("som");

        ProblemDetail problemDetail = updateProfileRequest(existingUser.getId(), existingUserToken, invalidRequest)
            .expectStatus().isEqualTo(422)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(422);
    }

    @Test
    void updateProfile_Unauthenticated_ShouldReturnUnauthorized() {
        UpdateProfileRequest updateRequest = createUpdateProfileRequest();

        restCli.patch()
            .uri("/api/v1/user/{id}", existingUser.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    private RestTestClient.ResponseSpec updateProfileRequest(UUID userId, String token,
                                                             UpdateProfileRequest request) {

        return restCli.patch()
            .uri("/api/v1/user/{id}", userId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange();
    }
}