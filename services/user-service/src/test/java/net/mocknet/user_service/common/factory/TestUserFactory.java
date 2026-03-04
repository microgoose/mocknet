package net.mocknet.user_service.common.factory;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.UpdateProfileRequest;
import net.mocknet.user_service.model.user.Role;
import net.mocknet.user_service.model.user.RoleName;
import net.mocknet.user_service.model.user.User;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Компонент для создания и управления тестовыми пользователями с уникальными данными.
 * Автоматически генерирует уникальные значения для всех полей.
 */
public final class TestUserFactory {

    // Базовые значения для генерации уникальных данных
    public static final String BASE_LOGIN = "testuser";
    public static final String BASE_EMAIL_DOMAIN = "@example.com";
    public static final String BASE_PASSWORD = "abcd1234";
    public static final String BASE_PASSWORD_HASH = "$2a$12$chbqsiwnuxF2LyePY9nsluXmefw.r9rpajnC/EMceEpLYKDyyWYUu";
    public static final String BASE_FIRST_NAME = "John";
    public static final String BASE_LAST_NAME = "Doe";
    public static final String BASE_AVATAR_PATH = "https://example.com/avatar";

    // Базовая роль для пользователей
    private static final Role BASE_ROLE = Role.builder()
        .id((short) 1)
        .name(RoleName.ROLE_USER)
        .build();

    private TestUserFactory() {
        // Приватный конструктор для предотвращения инстанциирования
    }

    /**
     * Создает нового пользователя с уникальными данными
     */
    public static User createUser() {
        UUID id = UUID.randomUUID();
        String postfix = id.toString().substring(0, 8);

        User user = new User();
        user.setId(id);
        user.setLogin(String.format("%s_%s", BASE_LOGIN, postfix));
        user.setEmail(String.format("%s_%s%s", BASE_LOGIN, postfix, BASE_EMAIL_DOMAIN));
        user.setPasswordHash(BASE_PASSWORD_HASH);
        user.setFirstName(String.format("%s_%s", BASE_FIRST_NAME, postfix));
        user.setLastName(String.format("%s_%s", BASE_LAST_NAME, postfix));
        user.setAvatarUrl(String.format("%s_%s.jpg", BASE_AVATAR_PATH, postfix));
        user.setActive(true);
        user.setVerified(true);
        user.setRoles(new HashSet<>(List.of(BASE_ROLE)));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setLastLoginAt(null);

        return user;
    }

    /**
     * Создает несколько пользователей
     */
    public static List<User> createUsers(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createUser())
            .collect(Collectors.toList());
    }

    /**
     * Создает уникальный RegisterRequestDto
     */
    public static RegisterRequestDto createRegisterRequest() {
        String postfix = UUID.randomUUID().toString().substring(0, 8);
        
        RegisterRequestDto request = new RegisterRequestDto();
        request.setLogin(String.format("%s_%s", BASE_LOGIN, postfix));
        request.setEmail(String.format("%s_%s%s", BASE_LOGIN, postfix, BASE_EMAIL_DOMAIN));
        request.setPassword(BASE_PASSWORD);
        request.setFirstName(String.format("%s_%s", BASE_FIRST_NAME, postfix));
        request.setLastName(String.format("%s_%s", BASE_LAST_NAME, postfix));

        return request;
    }

    /**
     * Создает уникальный ProfileDto
     */
    public static ProfileDto createProfileDto() {
        String postfix = UUID.randomUUID().toString().substring(0, 8);

        ProfileDto profile = new ProfileDto();
        profile.setId(UUID.randomUUID());
        profile.setLogin(String.format("%s_%s", BASE_LOGIN, postfix));
        profile.setEmail(String.format("%s_%s%s", BASE_LOGIN, postfix, BASE_EMAIL_DOMAIN));

        return profile;
    }

    /**
     * Создает уникальный UpdateProfileDto
     */
    public static UpdateProfileRequest createUpdateProfileRequest() {
        String postfix = UUID.randomUUID().toString().substring(0, 8);

        UpdateProfileRequest updateProfile = new UpdateProfileRequest();
        updateProfile.setLogin(String.format("%s_%s", BASE_LOGIN, postfix));
        updateProfile.setEmail(String.format("%s_%s%s", BASE_LOGIN, postfix, BASE_EMAIL_DOMAIN));
        updateProfile.setFirstName(String.format("%s_%s", BASE_FIRST_NAME, postfix));
        updateProfile.setLastName(String.format("%s_%s", BASE_LAST_NAME, postfix));
        updateProfile.setAvatarUrl(String.format("%s_%s.jpg", BASE_AVATAR_PATH, postfix));

        return updateProfile;
    }
}