package unit.service.user;

import common.TestEmailTokenFactory;
import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.dto.UpdateProfileDto;
import net.mocknet.user_service.exception.domain.auth.EmailAlreadyExistsException;
import net.mocknet.user_service.exception.domain.auth.LoginAlreadyExistsException;
import net.mocknet.user_service.exception.domain.role.RoleNotFoundException;
import net.mocknet.user_service.exception.domain.user.UserNotFound;
import net.mocknet.user_service.infrastructure.user.UserEventProducer;
import net.mocknet.user_service.mapper.UserMapper;
import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.user.Role;
import net.mocknet.user_service.model.user.RoleName;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.RoleRepository;
import net.mocknet.user_service.repository.UserRepository;
import net.mocknet.user_service.service.email.EmailTokenService;
import net.mocknet.user_service.service.user.UserEmailService;
import net.mocknet.user_service.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static common.TestEmailTokenFactory.createEmailToken;
import static common.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEmailService userEmailService;

    @Mock
    private UserEventProducer userEventProducer;

    @Mock
    private EmailTokenService emailTokenService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private RegisterRequestDto registerRequest;
    private UpdateProfileDto updateProfileRequest;
    private ProfileDto profileDto;
    private RegisterResponseDto registerResponseDto;
    private Role userRole;
    private EmailToken testEmailToken;
    private UUID testTokenUuid;

    @BeforeEach
    void setUp() {
        testUser = createUser();
        testUser.setVerified(true); // По умолчанию пользователь верифицирован
        testUserId = testUser.getId();
        testTokenUuid = TestEmailTokenFactory.TOKEN;
        
        userRole = Role.builder()
            .name(RoleName.ROLE_USER)
            .build();

        registerRequest = new RegisterRequestDto();
        registerRequest.setLogin("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");

        updateProfileRequest = new UpdateProfileDto();
        updateProfileRequest.setLogin("updatedlogin");
        updateProfileRequest.setEmail("updated@example.com");
        updateProfileRequest.setFirstName("Updated");
        updateProfileRequest.setLastName("Name");
        updateProfileRequest.setAvatarUrl("https://example.com/new-avatar.jpg");

        profileDto = new ProfileDto();
        profileDto.setId(testUserId);
        profileDto.setLogin(testUser.getLogin());
        profileDto.setEmail(testUser.getEmail());

        registerResponseDto = RegisterResponseDto.builder()
            .id(testUserId)
            .build();

        testEmailToken = createEmailToken();
    }

    @Nested
    class GetUserTests {

        @Test
        void getUser_WhenUserExists_ShouldReturnUser() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // Act
            User result = userService.getUser(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            verify(userRepository, times(1)).findById(testUserId);
        }

        @Test
        void getUser_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUser(testUserId))
                .isInstanceOf(UserNotFound.class);

            verify(userRepository, times(1)).findById(testUserId);
        }
    }

    @Nested
    class GetProfileTests {

        @Test
        void getProfile_WhenUserExists_ShouldReturnProfileDto() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userMapper.toProfileDto(testUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.getProfile(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            verify(userRepository, times(1)).findById(testUserId);
            verify(userMapper, times(1)).toProfileDto(testUser);
        }

        @Test
        void getProfile_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getProfile(testUserId))
                .isInstanceOf(UserNotFound.class);
        }
    }

    @Nested
    class RegisterTests {
        @Test
        void register_WhenNewUser_ShouldCreateUserAndSendVerification() {
            // Arrange
            String encodedPassword = "encodedPassword123";

            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(userRepository.findByLogin(registerRequest.getLogin())).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
            when(userMapper.toEntity(registerRequest, encodedPassword)).thenReturn(testUser);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toRegisterResponseDto(testUser)).thenReturn(registerResponseDto);

            // Act
            RegisterResponseDto result = userService.register(registerRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);

            verify(userRepository).findByEmail(registerRequest.getEmail());
            verify(userRepository).findByLogin(registerRequest.getLogin());
            verify(roleRepository).findByName(RoleName.ROLE_USER);
            verify(passwordEncoder).encode(registerRequest.getPassword());
            verify(userMapper).toEntity(registerRequest, encodedPassword);
            verify(userRepository).save(testUser);
            verify(userEmailService).verifyEmail(testUser);
            verify(userEventProducer).publishUserRegistered(testUser);
        }

        @Test
        void register_WhenEmailExistsWithVerifiedUser_ShouldThrowEmailAlreadyExistsException() {
            // Arrange
            User existingUser = createUser();
            existingUser.setVerified(true);
            
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(existingUser));
            when(userRepository.findByLogin(registerRequest.getLogin())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository).findByEmail(registerRequest.getEmail());
            verify(userRepository, never()).save(any());
            verify(userEmailService, never()).verifyEmail(any());
            verify(userEventProducer, never()).publishUserRegistered(any());
        }

        @Test
        void register_WhenLoginExistsWithVerifiedUser_ShouldThrowLoginAlreadyExistsException() {
            // Arrange
            User existingUser = createUser();
            existingUser.setVerified(true);
            
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(userRepository.findByLogin(registerRequest.getLogin())).thenReturn(Optional.of(existingUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(LoginAlreadyExistsException.class);

            verify(userRepository).findByEmail(registerRequest.getEmail());
            verify(userRepository).findByLogin(registerRequest.getLogin());
            verify(userRepository, never()).save(any());
            verify(userEmailService, never()).verifyEmail(any());
        }

        @Test
        void register_WhenEmailAndLoginBelongToDifferentUnverifiedUsers_ShouldDeleteLoginUserAndUpdateExisting() {
            // Arrange
            User userByEmail = createUser();
            userByEmail.setVerified(false);

            User userByLogin = createUser();
            userByLogin.setId(UUID.randomUUID());
            userByLogin.setVerified(false);

            String encodedPassword = "encodedPassword123";

            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(userByEmail));
            when(userRepository.findByLogin(registerRequest.getLogin())).thenReturn(Optional.of(userByLogin));
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
            when(userRepository.save(userByEmail)).thenReturn(userByEmail);
            when(userMapper.toRegisterResponseDto(userByEmail)).thenReturn(registerResponseDto);

            // Act
            RegisterResponseDto result = userService.register(registerRequest);

            // Assert
            assertThat(result).isNotNull();

            InOrder inOrder = inOrder(emailTokenService, userRepository, userEmailService);

            inOrder.verify(emailTokenService).deleteAllByUser(userByLogin);
            inOrder.verify(userRepository).delete(userByLogin);
            inOrder.verify(userRepository).save(userByEmail);
            inOrder.verify(userEmailService).verifyEmail(userByEmail);

            // Verify that userByEmail was updated with new data
            assertThat(userByEmail.getLogin()).isEqualTo(registerRequest.getLogin());
            assertThat(userByEmail.getEmail()).isEqualTo(registerRequest.getEmail());
            assertThat(userByEmail.getPasswordHash()).isEqualTo(encodedPassword);
            assertThat(userByEmail.getFirstName()).isEqualTo(registerRequest.getFirstName());
            assertThat(userByEmail.getLastName()).isEqualTo(registerRequest.getLastName());

            // Verify that role repository was never called (we're updating, not creating)
            verify(roleRepository, never()).findByName(any());

            // Verify that new user event is not published (user already existed)
            verify(userEventProducer, never()).publishUserRegistered(any());
        }

        @Test
        void register_WhenEmailExistsWithUnverifiedUser_ShouldUpdateExistingUser() {
            // Arrange
            User existingUser = createUser();
            existingUser.setVerified(false);
            
            String encodedPassword = "encodedPassword123";
            
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(existingUser));
            when(userRepository.findByLogin(registerRequest.getLogin())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
            when(userRepository.save(existingUser)).thenReturn(existingUser);
            when(userMapper.toRegisterResponseDto(existingUser)).thenReturn(registerResponseDto);

            // Act
            RegisterResponseDto result = userService.register(registerRequest);

            // Assert
            assertThat(result).isNotNull();
            
            assertThat(existingUser.getLogin()).isEqualTo(registerRequest.getLogin());
            assertThat(existingUser.getEmail()).isEqualTo(registerRequest.getEmail());
            assertThat(existingUser.getPasswordHash()).isEqualTo(encodedPassword);
            assertThat(existingUser.getFirstName()).isEqualTo(registerRequest.getFirstName());
            assertThat(existingUser.getLastName()).isEqualTo(registerRequest.getLastName());
            
            verify(userRepository).save(existingUser);
            verify(userEmailService).verifyEmail(existingUser);
            verify(userEventProducer, never()).publishUserRegistered(any());
        }

        @Test
        void register_WhenRoleNotFound_ShouldThrowRoleNotFoundException() {
            // Arrange
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(userRepository.findByLogin(registerRequest.getLogin())).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(RoleNotFoundException.class);

            verify(userRepository, never()).save(any());
            verify(userEmailService, never()).verifyEmail(any());
        }
    }

    @Nested
    class AcceptVerificationTests {

        @Test
        void acceptVerification_WithValidToken_ShouldVerifyUser() {
            // Arrange
            testUser.setVerified(false);
            testEmailToken.setUser(testUser);
            
            when(emailTokenService.getValidToken(testTokenUuid)).thenReturn(testEmailToken);
            when(userRepository.save(testUser)).thenReturn(testUser);

            // Act
            userService.acceptVerification(testTokenUuid);

            // Assert
            InOrder inOrder = inOrder(emailTokenService, userRepository);
            
            inOrder.verify(emailTokenService).getValidToken(testTokenUuid);
            inOrder.verify(emailTokenService).markAsUsed(testEmailToken);
            inOrder.verify(userRepository).save(testUser);
            
            assertThat(testUser.isVerified()).isTrue();
        }

        @Test
        void acceptVerification_WhenTokenInvalid_ShouldPropagateException() {
            // Arrange
            when(emailTokenService.getValidToken(testTokenUuid))
                .thenThrow(new RuntimeException("Invalid token"));

            // Act & Assert
            assertThatThrownBy(() -> userService.acceptVerification(testTokenUuid))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid token");

            verify(emailTokenService).getValidToken(testTokenUuid);
            verify(emailTokenService, never()).markAsUsed(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateProfileTests {

        @Test
        void updateProfile_WithAllFieldsChanged_ShouldUpdateAllFields() {
            // Arrange
            // Сбрасываем verified в true перед тестом, так как в setUp он true
            testUser.setVerified(true);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByLogin(updateProfileRequest.getLogin())).thenReturn(false);
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toProfileDto(testUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(testUserId, updateProfileRequest);

            // Assert
            assertThat(result).isNotNull();
            
            assertThat(testUser.getLogin()).isEqualTo(updateProfileRequest.getLogin());
            assertThat(testUser.getEmail()).isEqualTo(updateProfileRequest.getEmail());
            assertThat(testUser.isVerified()).isFalse(); // Должен стать false после смены email
            assertThat(testUser.getFirstName()).isEqualTo(updateProfileRequest.getFirstName());
            assertThat(testUser.getLastName()).isEqualTo(updateProfileRequest.getLastName());
            assertThat(testUser.getAvatarUrl()).isEqualTo(updateProfileRequest.getAvatarUrl());
            
            verify(userRepository).findById(testUserId);
            verify(userRepository).existsByLogin(updateProfileRequest.getLogin());
            verify(userEmailService).verifyEmail(testUser);
            verify(userRepository).save(testUser);
            verify(userMapper).toProfileDto(testUser);
        }

        @Test
        void updateProfile_WhenLoginExists_ShouldThrowLoginAlreadyExistsException() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByLogin(updateProfileRequest.getLogin())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(testUserId, updateProfileRequest))
                .isInstanceOf(LoginAlreadyExistsException.class);

            verify(userRepository).findById(testUserId);
            verify(userRepository).existsByLogin(updateProfileRequest.getLogin());
            verify(userRepository, never()).save(any());
            verify(userEmailService, never()).verifyEmail(any());
        }

        @Test
        void updateProfile_WhenOnlyFirstNameChanged_ShouldUpdateOnlyFirstName() {
            // Arrange
            UpdateProfileDto request = new UpdateProfileDto();
            request.setFirstName("NewFirstName");
            
            String originalLogin = testUser.getLogin();
            String originalEmail = testUser.getEmail();
            String originalLastName = testUser.getLastName();
            String originalAvatarUrl = testUser.getAvatarUrl();
            boolean originalVerified = testUser.isVerified();
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toProfileDto(testUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(testUserId, request);

            // Assert
            assertThat(testUser.getLogin()).isEqualTo(originalLogin);
            assertThat(testUser.getEmail()).isEqualTo(originalEmail);
            assertThat(testUser.getFirstName()).isEqualTo("NewFirstName");
            assertThat(testUser.getLastName()).isEqualTo(originalLastName);
            assertThat(testUser.getAvatarUrl()).isEqualTo(originalAvatarUrl);
            assertThat(testUser.isVerified()).isEqualTo(originalVerified); // verified не должен измениться
            
            verify(userRepository, never()).existsByLogin(any());
            verify(userEmailService, never()).verifyEmail(any());
            verify(userRepository).save(testUser);
        }

        @Test
        void updateProfile_WhenEmailChanged_ShouldSetVerifiedFalseAndSendVerification() {
            // Arrange
            UpdateProfileDto request = new UpdateProfileDto();
            request.setEmail("newemail@example.com");
            
            testUser.setVerified(true);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toProfileDto(testUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(testUserId, request);

            // Assert
            assertThat(testUser.getEmail()).isEqualTo("newemail@example.com");
            assertThat(testUser.isVerified()).isFalse();
            
            verify(userEmailService).verifyEmail(testUser);
            verify(userRepository).save(testUser);
        }

        @Test
        void updateProfile_WhenLoginChangedToSameValue_ShouldNotCheckExistence() {
            // Arrange
            UpdateProfileDto request = new UpdateProfileDto();
            request.setLogin(testUser.getLogin()); // Тот же логин
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toProfileDto(testUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(testUserId, request);

            // Assert
            verify(userRepository, never()).existsByLogin(any());
            verify(userRepository).save(testUser);
        }

        @Test
        void updateProfile_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(testUserId, updateProfileRequest))
                .isInstanceOf(UserNotFound.class);
        }
    }

    @Nested
    class UpdateLastLoginTests {

        @Test
        void updateLastLogin_WithValidTime_ShouldUpdateLastLogin() {
            // Arrange
            OffsetDateTime lastLoginAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // Act
            userService.updateLastLogin(testUserId, lastLoginAt);

            // Assert
            assertThat(testUser.getLastLoginAt()).isEqualTo(lastLoginAt);
            verify(userRepository).findById(testUserId);
        }

        @Test
        void updateLastLogin_WhenNewTimeIsBeforeExisting_ShouldThrowIllegalArgumentException() {
            // Arrange
            OffsetDateTime existingLoginAt = OffsetDateTime.now();
            testUser.setLastLoginAt(existingLoginAt);
            
            OffsetDateTime earlierLoginAt = existingLoginAt.minusHours(1);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.updateLastLogin(testUserId, earlierLoginAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThat(testUser.getLastLoginAt()).isEqualTo(existingLoginAt);
        }

        @Test
        void updateLastLogin_WhenLastLoginWasNull_ShouldSetLastLogin() {
            // Arrange
            testUser.setLastLoginAt(null);
            OffsetDateTime lastLoginAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // Act
            userService.updateLastLogin(testUserId, lastLoginAt);

            // Assert
            assertThat(testUser.getLastLoginAt()).isEqualTo(lastLoginAt);
        }

        @Test
        void updateLastLogin_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateLastLogin(testUserId, OffsetDateTime.now()))
                .isInstanceOf(UserNotFound.class);
        }
    }
}