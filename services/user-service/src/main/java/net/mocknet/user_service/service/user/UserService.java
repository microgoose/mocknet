package net.mocknet.user_service.service.user;

import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.UpdateProfileDto;
import net.mocknet.user_service.exception.domain.auth.LoginAlreadyExistsException;
import net.mocknet.user_service.exception.domain.user.UserNotFound;
import net.mocknet.user_service.mapper.UserMapper;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEmailService userEmailService;

    public User getUser(UUID id) {
        return userRepository
            .findById(id)
            .orElseThrow(() -> new UserNotFound(id));
    }

    public ProfileDto getProfile(UUID id) {
        return userMapper.toProfileDto(getUser(id));
    }

    @Transactional
    public ProfileDto updateProfile(UUID id, UpdateProfileDto request) {
        User user = getUser(id);

        if (request.getLogin() != null && !Objects.equals(request.getLogin(), user.getLogin())) {
            if (userRepository.existsByLogin(request.getLogin()))
                throw new LoginAlreadyExistsException(request.getLogin());

            user.setLogin(request.getLogin());
        }

        if (request.getEmail() != null && !Objects.equals(request.getEmail(), user.getEmail())) {
            user.setEmail(request.getEmail());
            user.setVerified(false);
            userEmailService.verifyEmail(user);
        }

        if (request.getFirstName() != null && !Objects.equals(request.getFirstName(), user.getFirstName()))
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null && !Objects.equals(request.getLastName(), user.getLastName()))
            user.setLastName(request.getLastName());
        if (request.getAvatarUrl() != null && !Objects.equals(request.getAvatarUrl(), user.getAvatarUrl()))
            user.setAvatarUrl(request.getAvatarUrl());

        return userMapper.toProfileDto(userRepository.save(user));
    }

    @Transactional
    public void updateLastLogin(UUID id, OffsetDateTime lastLoginAt) {
        User user = getUser(id);

        if (user.getLastLoginAt() != null && lastLoginAt.isBefore(user.getLastLoginAt())) {
            throw new IllegalArgumentException("Время авторизации не может быть меньше существующего значения");
        }

        user.setLastLoginAt(lastLoginAt);
    }
}
