package net.mocknet.user_service.mapper;

import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.model.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public ProfileDto toProfileDto(User user) {
        ProfileDto profileDto = new ProfileDto();
        profileDto.setId(user.getId());
        profileDto.setLogin(user.getLogin());
        profileDto.setEmail(user.getEmail());
        profileDto.setFirstName(user.getFirstName());
        profileDto.setLastName(user.getLastName());
        profileDto.setAvatarUrl(user.getAvatarUrl());
        profileDto.setLastLoginAt(user.getLastLoginAt());
        return profileDto;
    }

    public User toEntity(RegisterRequestDto dto, String passwordHash) {
        User user = new User();
        user.setLogin(dto.getLogin());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordHash);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        return user;
    }

    public RegisterResponseDto toRegisterResponseDto(User user) {
        return RegisterResponseDto.builder()
            .id(user.getId())
            .login(user.getLogin())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .isVerified(user.isVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
