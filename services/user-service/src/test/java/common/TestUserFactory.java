package common;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.UpdateProfileDto;
import net.mocknet.user_service.model.user.Role;
import net.mocknet.user_service.model.user.RoleName;
import net.mocknet.user_service.model.user.User;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestUserFactory {

    public static final UUID ID = UUID.randomUUID();
    public static final String LOGIN = "testuser";
    public static final String EMAIL = "test@example.com";
    public static final String PASSWORD = "100";
    public static final String PASSWORD_HASH = "$2a$12$chbqsiwnuxF2LyePY9nsluXmefw.r9rpajnC/EMceEpLYKDyyWYUu";
    public static final String FIRST_NAME = "John";
    public static final String LAST_NAME = "Doe";
    public static final String AVATAR_URL = "https://example.com/avatar.jpg";
    public static final boolean IS_ACTIVE = true;
    public static final boolean IS_VERIFIED = true;
    public static final OffsetDateTime CREATED_AT = OffsetDateTime.now();
    public static final OffsetDateTime UPDATED_AT = OffsetDateTime.now();
    public static final OffsetDateTime LAST_LOGIN_AT = null;

    public static final Role ROLE_USER = Role.builder()
        .name(RoleName.ROLE_USER)
        .build();

    public static final Set<Role> ROLES = new HashSet<>(List.of(ROLE_USER));

    public static User createUser() {
        User user = new User();
        user.setId(ID);
        user.setLogin(LOGIN);
        user.setEmail(EMAIL);
        user.setPasswordHash(PASSWORD_HASH);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setAvatarUrl(AVATAR_URL);
        user.setActive(IS_ACTIVE);
        user.setVerified(IS_VERIFIED);
        user.setRoles(ROLES);
        user.setCreatedAt(CREATED_AT);
        user.setUpdatedAt(UPDATED_AT);
        user.setLastLoginAt(LAST_LOGIN_AT);
        return user;
    }

    public static RegisterRequestDto createRegisterRequest() {
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setLogin(LOGIN);
        registerRequest.setEmail(EMAIL);
        registerRequest.setPassword(PASSWORD);
        registerRequest.setFirstName(FIRST_NAME);
        registerRequest.setLastName(LAST_NAME);
        return registerRequest;
    }

    public static ProfileDto createProfileDto() {
        ProfileDto profileDto = new ProfileDto();
        profileDto.setId(ID);
        profileDto.setLogin(LOGIN);
        profileDto.setEmail(EMAIL);
        return profileDto;
    }

    public static UpdateProfileDto createUpdateProfile() {
        UpdateProfileDto updateProfileRequest = new UpdateProfileDto();
        updateProfileRequest.setLogin(LOGIN);
        updateProfileRequest.setEmail(EMAIL);
        updateProfileRequest.setFirstName(FIRST_NAME);
        updateProfileRequest.setLastName(LAST_NAME);
        updateProfileRequest.setAvatarUrl(AVATAR_URL);
        return updateProfileRequest;
    }
}