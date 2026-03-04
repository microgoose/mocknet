package net.mocknet.user_service.unit.mapper;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.mapper.UserMapper;
import net.mocknet.user_service.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @InjectMocks
    private UserMapper userMapper;

    private User testUser;
    private RegisterRequestDto registerRequest;
    private String testPasswordHash;
    private UUID testUserId;
    private OffsetDateTime testCreatedAt;
    private OffsetDateTime testUpdatedAt;
    private OffsetDateTime testLastLoginAt;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testCreatedAt = OffsetDateTime.now().minusDays(1);
        testUpdatedAt = OffsetDateTime.now();
        testLastLoginAt = OffsetDateTime.now().minusHours(5);
        testPasswordHash = "$2a$10$test.hash.value";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setLogin("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(testPasswordHash);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setActive(true);
        testUser.setVerified(true);
        testUser.setCreatedAt(testCreatedAt);
        testUser.setUpdatedAt(testUpdatedAt);
        testUser.setLastLoginAt(testLastLoginAt);

        registerRequest = new RegisterRequestDto();
        registerRequest.setLogin("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("plainpassword");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");
    }

    @Nested
    class ToProfileDtoTests {

        @Test
        void toProfileDto_WithAllFields_ShouldMapAllFieldsCorrectly() {
            // Act
            ProfileDto result = userMapper.toProfileDto(testUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getLogin()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(result.getLastLoginAt()).isEqualTo(testLastLoginAt);
        }

        @Test
        void toProfileDto_WithNullOptionalFields_ShouldMapWithNulls() {
            // Arrange
            testUser.setFirstName(null);
            testUser.setLastName(null);
            testUser.setAvatarUrl(null);
            testUser.setLastLoginAt(null);

            // Act
            ProfileDto result = userMapper.toProfileDto(testUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getLogin()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
            assertThat(result.getAvatarUrl()).isNull();
            assertThat(result.getLastLoginAt()).isNull();
        }

        @Test
        void toProfileDto_WithEmptyStrings_ShouldMapEmptyStrings() {
            // Arrange
            testUser.setFirstName("");
            testUser.setLastName("");
            testUser.setAvatarUrl("");

            // Act
            ProfileDto result = userMapper.toProfileDto(testUser);

            // Assert
            assertThat(result.getFirstName()).isEmpty();
            assertThat(result.getLastName()).isEmpty();
            assertThat(result.getAvatarUrl()).isEmpty();
        }

        @Test
        void toProfileDto_WithNullUser_ShouldThrowException() {
            // Act & Assert
            assertThatThrownBy(() -> userMapper.toProfileDto(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class ToEntityTests {

        @Test
        void toEntity_WithAllFields_ShouldCreateUserWithCorrectFields() {
            // Act
            User result = userMapper.toEntity(registerRequest, testPasswordHash);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNull(); // ID должен быть null, т.к. будет сгенерирован при сохранении
            assertThat(result.getLogin()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getPasswordHash()).isEqualTo(testPasswordHash);
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            
            // Поля с значениями по умолчанию
            assertThat(result.getAvatarUrl()).isNull();
            assertThat(result.isActive()).isTrue(); // По умолчанию true
            assertThat(result.isVerified()).isFalse(); // По умолчанию false
            assertThat(result.getRoles()).isEmpty();
            assertThat(result.getCreatedAt()).isNull();
            assertThat(result.getUpdatedAt()).isNull();
            assertThat(result.getLastLoginAt()).isNull();
        }

        @Test
        void toEntity_WithNullFirstNameAndLastName_ShouldMapNulls() {
            // Arrange
            registerRequest.setFirstName(null);
            registerRequest.setLastName(null);

            // Act
            User result = userMapper.toEntity(registerRequest, testPasswordHash);

            // Assert
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
        }

        @Test
        void toEntity_WithEmptyStrings_ShouldMapEmptyStrings() {
            // Arrange
            registerRequest.setFirstName("");
            registerRequest.setLastName("");

            // Act
            User result = userMapper.toEntity(registerRequest, testPasswordHash);

            // Assert
            assertThat(result.getFirstName()).isEmpty();
            assertThat(result.getLastName()).isEmpty();
        }

        @Test
        void toEntity_WithNullRegisterRequest_ShouldThrowException() {
            // Act & Assert
            assertThatThrownBy(() -> userMapper.toEntity(null, testPasswordHash))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void toEntity_WithNullPasswordHash_ShouldSetNull() {
            // Act
            User result = userMapper.toEntity(registerRequest, null);

            // Assert
            assertThat(result.getPasswordHash()).isNull();
        }

        @Test
        void toEntity_ShouldNotSetTimestampsOrIds() {
            // Act
            User result = userMapper.toEntity(registerRequest, testPasswordHash);

            // Assert
            assertThat(result.getId()).isNull();
            assertThat(result.getCreatedAt()).isNull();
            assertThat(result.getUpdatedAt()).isNull();
            assertThat(result.getLastLoginAt()).isNull();
        }
    }

    @Nested
    class ToRegisterResponseDtoTests {

        @Test
        void toRegisterResponseDto_WithAllFields_ShouldMapAllFieldsCorrectly() {
            // Act
            RegisterResponseDto result = userMapper.toRegisterResponseDto(testUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getLogin()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getIsVerified()).isTrue();
            assertThat(result.getCreatedAt()).isEqualTo(testCreatedAt);
        }

        @Test
        void toRegisterResponseDto_WithNullOptionalFields_ShouldMapWithNulls() {
            // Arrange
            testUser.setFirstName(null);
            testUser.setLastName(null);

            // Act
            RegisterResponseDto result = userMapper.toRegisterResponseDto(testUser);

            // Assert
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
        }

        @Test
        void toRegisterResponseDto_WithUnverifiedUser_ShouldMapVerifiedFalse() {
            // Arrange
            testUser.setVerified(false);

            // Act
            RegisterResponseDto result = userMapper.toRegisterResponseDto(testUser);

            // Assert
            assertThat(result.getIsVerified()).isFalse();
        }

        @Test
        void toRegisterResponseDto_WithBuilder_ShouldCreateValidDto() {
            // Act
            RegisterResponseDto result = userMapper.toRegisterResponseDto(testUser);

            // Assert
            assertThat(result).isNotNull();
            
            // Проверяем, что используется билдер (должен создать валидный объект)
            assertThat(result.getId()).isNotNull();
            assertThat(result.getLogin()).isNotNull();
            assertThat(result.getEmail()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        void toRegisterResponseDto_WithNullUser_ShouldThrowException() {
            // Act & Assert
            assertThatThrownBy(() -> userMapper.toRegisterResponseDto(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class IntegrationTests {

        @Test
        void toEntityAndToProfileDto_ShouldPreserveData() {
            // Arrange
            RegisterRequestDto request = new RegisterRequestDto();
            request.setLogin("net/mocknet/user_service/integration");
            request.setEmail("integration@example.com");
            request.setPassword("password");
            request.setFirstName("Integration");
            request.setLastName("Test");

            String passwordHash = "$2a$10$integration.hash";

            // Act - toEntity
            User user = userMapper.toEntity(request, passwordHash);
            
            // Устанавливаем поля, которые обычно устанавливаются при сохранении
            user.setId(UUID.randomUUID());
            user.setVerified(true);
            user.setActive(true);
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());

            // Act - toProfileDto
            ProfileDto profileDto = userMapper.toProfileDto(user);

            // Assert
            assertThat(profileDto.getId()).isEqualTo(user.getId());
            assertThat(profileDto.getLogin()).isEqualTo("net/mocknet/user_service/integration");
            assertThat(profileDto.getEmail()).isEqualTo("integration@example.com");
            assertThat(profileDto.getFirstName()).isEqualTo("Integration");
            assertThat(profileDto.getLastName()).isEqualTo("Test");
        }

        @Test
        void toEntityAndToRegisterResponseDto_ShouldPreserveData() {
            // Arrange
            RegisterRequestDto request = new RegisterRequestDto();
            request.setLogin("net/mocknet/user_service/integration");
            request.setEmail("integration@example.com");
            request.setPassword("password");
            request.setFirstName("Integration");
            request.setLastName("Test");

            String passwordHash = "$2a$10$integration.hash";

            // Act - toEntity
            User user = userMapper.toEntity(request, passwordHash);
            
            // Устанавливаем поля, которые обычно устанавливаются при сохранении
            UUID userId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            user.setId(userId);
            user.setVerified(true);
            user.setCreatedAt(now);

            // Act - toRegisterResponseDto
            RegisterResponseDto responseDto = userMapper.toRegisterResponseDto(user);

            // Assert
            assertThat(responseDto.getId()).isEqualTo(userId);
            assertThat(responseDto.getLogin()).isEqualTo("net/mocknet/user_service/integration");
            assertThat(responseDto.getEmail()).isEqualTo("integration@example.com");
            assertThat(responseDto.getFirstName()).isEqualTo("Integration");
            assertThat(responseDto.getLastName()).isEqualTo("Test");
            assertThat(responseDto.getIsVerified()).isTrue();
            assertThat(responseDto.getCreatedAt()).isEqualTo(now);
        }

        @Test
        void toEntity_WithEmptyRequest_ShouldCreateUserWithMinimalFields() {
            // Arrange
            RegisterRequestDto minimalRequest = new RegisterRequestDto();
            minimalRequest.setLogin("minimal");
            minimalRequest.setEmail("minimal@example.com");
            minimalRequest.setPassword("pass");
            // firstName и lastName не установлены

            // Act
            User user = userMapper.toEntity(minimalRequest, "hash");

            // Assert
            assertThat(user.getLogin()).isEqualTo("minimal");
            assertThat(user.getEmail()).isEqualTo("minimal@example.com");
            assertThat(user.getPasswordHash()).isEqualTo("hash");
            assertThat(user.getFirstName()).isNull();
            assertThat(user.getLastName()).isNull();
        }
    }
}