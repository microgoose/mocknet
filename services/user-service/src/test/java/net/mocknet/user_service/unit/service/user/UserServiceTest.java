package net.mocknet.user_service.unit.service.user;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.dto.UpdateProfileRequest;
import net.mocknet.user_service.exception.domain.auth.EmailAlreadyExistsException;
import net.mocknet.user_service.exception.domain.auth.LoginAlreadyExistsException;
import net.mocknet.user_service.exception.domain.role.RoleNotFoundException;
import net.mocknet.user_service.exception.domain.user.UserNotFound;
import net.mocknet.user_service.exception.domain.user.UserUpdateForbidden;
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

import static net.mocknet.user_service.common.factory.TestEmailTokenFactory.createEmailToken;
import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
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

    private User activeUser;
    private RegisterRequestDto registerRequest;
    private UpdateProfileRequest updateProfileRequest;
    private ProfileDto profileDto;
    private RegisterResponseDto registerResponseDto;
    private Role userRole;
    private EmailToken testEmailToken;

    @BeforeEach
    void setUp() {
        activeUser = createUser();
        activeUser.setVerified(true); // По умолчанию пользователь верифицирован
        
        userRole = Role.builder()
            .name(RoleName.ROLE_USER)
            .build();

        registerRequest = new RegisterRequestDto();
        registerRequest.setLogin("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");

        updateProfileRequest = new UpdateProfileRequest();
        updateProfileRequest.setLogin("updatedlogin");
        updateProfileRequest.setEmail("updated@example.com");
        updateProfileRequest.setFirstName("Updated");
        updateProfileRequest.setLastName("Name");
        updateProfileRequest.setAvatarUrl("https://example.com/new-avatar.jpg");

        profileDto = new ProfileDto();
        profileDto.setId(activeUser.getId());
        profileDto.setLogin(activeUser.getLogin());
        profileDto.setEmail(activeUser.getEmail());

        registerResponseDto = RegisterResponseDto.builder()
            .id(activeUser.getId())
            .build();

        testEmailToken = createEmailToken(activeUser);
    }

    @Nested
    class GetUserTests {

        @Test
        void getUser_WhenUserExists_ShouldReturnUser() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            // Act
            User result = userService.getUser(activeUser.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(activeUser.getId());
            verify(userRepository, times(1)).findById(activeUser.getId());
        }

        @Test
        void getUser_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUser(activeUser.getId()))
                .isInstanceOf(UserNotFound.class);

            verify(userRepository, times(1)).findById(activeUser.getId());
        }
    }

    @Nested
    class GetProfileTests {

        @Test
        void getProfile_WhenUserExists_ShouldReturnProfileDto() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userMapper.toProfileDto(activeUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.getProfile(activeUser.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(activeUser.getId());
            verify(userRepository, times(1)).findById(activeUser.getId());
            verify(userMapper, times(1)).toProfileDto(activeUser);
        }

        @Test
        void getProfile_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getProfile(activeUser.getId()))
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
            when(userMapper.toEntity(registerRequest, encodedPassword)).thenReturn(activeUser);
            when(userRepository.save(any(User.class))).thenReturn(activeUser);
            when(userMapper.toRegisterResponseDto(activeUser)).thenReturn(registerResponseDto);

            // Act
            RegisterResponseDto result = userService.register(registerRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(activeUser.getId());

            verify(userRepository).findByEmail(registerRequest.getEmail());
            verify(userRepository).findByLogin(registerRequest.getLogin());
            verify(roleRepository).findByName(RoleName.ROLE_USER);
            verify(passwordEncoder).encode(registerRequest.getPassword());
            verify(userMapper).toEntity(registerRequest, encodedPassword);
            verify(userRepository).save(activeUser);
            verify(userEmailService).verifyEmail(activeUser);
            verify(userEventProducer).publishUserRegistered(activeUser);
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
            activeUser.setVerified(false);
            testEmailToken.setUser(activeUser);
            
            when(emailTokenService.getValidToken(testEmailToken.getToken())).thenReturn(testEmailToken);
            when(userRepository.save(activeUser)).thenReturn(activeUser);

            // Act
            userService.acceptVerification(testEmailToken.getToken());

            // Assert
            InOrder inOrder = inOrder(emailTokenService, userRepository);
            
            inOrder.verify(emailTokenService).getValidToken(testEmailToken.getToken());
            inOrder.verify(emailTokenService).markAsUsed(testEmailToken);
            inOrder.verify(userRepository).save(activeUser);
            
            assertThat(activeUser.isVerified()).isTrue();
        }

        @Test
        void acceptVerification_WhenTokenInvalid_ShouldPropagateException() {
            // Arrange
            when(emailTokenService.getValidToken(testEmailToken.getToken()))
                .thenThrow(new RuntimeException("Invalid token"));

            // Act & Assert
            assertThatThrownBy(() -> userService.acceptVerification(testEmailToken.getToken()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid token");

            verify(emailTokenService).getValidToken(testEmailToken.getToken());
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
            activeUser.setVerified(true);
            
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByLogin(updateProfileRequest.getLogin())).thenReturn(false);
            when(userRepository.save(activeUser)).thenReturn(activeUser);
            when(userMapper.toProfileDto(activeUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(activeUser.getId(), updateProfileRequest, activeUser.getId());

            // Assert
            assertThat(result).isNotNull();
            
            assertThat(activeUser.getLogin()).isEqualTo(updateProfileRequest.getLogin());
            assertThat(activeUser.getEmail()).isEqualTo(updateProfileRequest.getEmail());
            assertThat(activeUser.isVerified()).isFalse(); // Должен стать false после смены email
            assertThat(activeUser.getFirstName()).isEqualTo(updateProfileRequest.getFirstName());
            assertThat(activeUser.getLastName()).isEqualTo(updateProfileRequest.getLastName());
            assertThat(activeUser.getAvatarUrl()).isEqualTo(updateProfileRequest.getAvatarUrl());
            
            verify(userRepository).findById(activeUser.getId());
            verify(userRepository).existsByLogin(updateProfileRequest.getLogin());
            verify(userEmailService).verifyEmail(activeUser);
            verify(userRepository).save(activeUser);
            verify(userMapper).toProfileDto(activeUser);
        }

        @Test
        void updateProfile_WhenLoginExists_ShouldThrowLoginAlreadyExistsException() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByLogin(updateProfileRequest.getLogin())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(activeUser.getId(), updateProfileRequest, activeUser.getId()))
                .isInstanceOf(LoginAlreadyExistsException.class);

            verify(userRepository).findById(activeUser.getId());
            verify(userRepository).existsByLogin(updateProfileRequest.getLogin());
            verify(userRepository, never()).save(any());
            verify(userEmailService, never()).verifyEmail(any());
        }

        @Test
        void updateProfile_WhenOnlyFirstNameChanged_ShouldUpdateOnlyFirstName() {
            // Arrange
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("NewFirstName");
            
            String originalLogin = activeUser.getLogin();
            String originalEmail = activeUser.getEmail();
            String originalLastName = activeUser.getLastName();
            String originalAvatarUrl = activeUser.getAvatarUrl();
            boolean originalVerified = activeUser.isVerified();
            
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.save(activeUser)).thenReturn(activeUser);
            when(userMapper.toProfileDto(activeUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(activeUser.getId(), request, activeUser.getId());

            // Assert
            assertThat(activeUser.getLogin()).isEqualTo(originalLogin);
            assertThat(activeUser.getEmail()).isEqualTo(originalEmail);
            assertThat(activeUser.getFirstName()).isEqualTo("NewFirstName");
            assertThat(activeUser.getLastName()).isEqualTo(originalLastName);
            assertThat(activeUser.getAvatarUrl()).isEqualTo(originalAvatarUrl);
            assertThat(activeUser.isVerified()).isEqualTo(originalVerified); // verified не должен измениться
            
            verify(userRepository, never()).existsByLogin(any());
            verify(userEmailService, never()).verifyEmail(any());
            verify(userRepository).save(activeUser);
        }

        @Test
        void updateProfile_WhenEmailChanged_ShouldSetVerifiedFalseAndSendVerification() {
            // Arrange
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setEmail("newemail@example.com");
            
            activeUser.setVerified(true);
            
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.save(activeUser)).thenReturn(activeUser);
            when(userMapper.toProfileDto(activeUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(activeUser.getId(), request, activeUser.getId());

            // Assert
            assertThat(activeUser.getEmail()).isEqualTo("newemail@example.com");
            assertThat(activeUser.isVerified()).isFalse();
            
            verify(userEmailService).verifyEmail(activeUser);
            verify(userRepository).save(activeUser);
        }

        @Test
        void updateProfile_WhenLoginChangedToSameValue_ShouldNotCheckExistence() {
            // Arrange
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setLogin(activeUser.getLogin()); // Тот же логин
            
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
            when(userRepository.save(activeUser)).thenReturn(activeUser);
            when(userMapper.toProfileDto(activeUser)).thenReturn(profileDto);

            // Act
            ProfileDto result = userService.updateProfile(activeUser.getId(), request, activeUser.getId());

            // Assert
            verify(userRepository, never()).existsByLogin(any());
            verify(userRepository).save(activeUser);
        }

        @Test
        void updateProfile_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(activeUser.getId(), updateProfileRequest, activeUser.getId()))
                .isInstanceOf(UserNotFound.class);
        }

        @Test
        void updateProfile_WhenUpdaterIsNotTheUser_ShouldThrowUserConflict() {
            UUID someUserId = UUID.randomUUID();
            assertThatThrownBy(() -> userService.updateProfile(activeUser.getId(), updateProfileRequest, someUserId))
                .isInstanceOf(UserUpdateForbidden.class);
        }
    }

    @Nested
    class UpdateLastLoginTests {

        @Test
        void updateLastLogin_WithValidTime_ShouldUpdateLastLogin() {
            // Arrange
            OffsetDateTime lastLoginAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            // Act
            userService.updateLastLogin(activeUser.getId(), lastLoginAt);

            // Assert
            assertThat(activeUser.getLastLoginAt()).isEqualTo(lastLoginAt);
            verify(userRepository).findById(activeUser.getId());
        }

        @Test
        void updateLastLogin_WhenNewTimeIsBeforeExisting_ShouldThrowIllegalArgumentException() {
            // Arrange
            OffsetDateTime existingLoginAt = OffsetDateTime.now();
            activeUser.setLastLoginAt(existingLoginAt);
            
            OffsetDateTime earlierLoginAt = existingLoginAt.minusHours(1);
            
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.updateLastLogin(activeUser.getId(), earlierLoginAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThat(activeUser.getLastLoginAt()).isEqualTo(existingLoginAt);
        }

        @Test
        void updateLastLogin_WhenLastLoginWasNull_ShouldSetLastLogin() {
            // Arrange
            activeUser.setLastLoginAt(null);
            OffsetDateTime lastLoginAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            // Act
            userService.updateLastLogin(activeUser.getId(), lastLoginAt);

            // Assert
            assertThat(activeUser.getLastLoginAt()).isEqualTo(lastLoginAt);
        }

        @Test
        void updateLastLogin_WhenUserNotFound_ShouldThrowUserNotFound() {
            // Arrange
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateLastLogin(activeUser.getId(), OffsetDateTime.now()))
                .isInstanceOf(UserNotFound.class);
        }
    }
}