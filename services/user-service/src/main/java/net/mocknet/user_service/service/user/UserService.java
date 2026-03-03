package net.mocknet.user_service.service.user;

import lombok.RequiredArgsConstructor;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEmailService userEmailService;
    private final UserEventProducer userEventProducer;
    private final EmailTokenService emailTokenService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFound(id));
    }

    public ProfileDto getProfile(UUID id) {
        return userMapper.toProfileDto(getUser(id));
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        Optional<User> byEmail = userRepository.findByEmail(request.getEmail());
        Optional<User> byLogin = userRepository.findByLogin(request.getLogin());

        // Если найден верифицированный пользователь — конфликт
        byEmail.filter(User::isVerified).ifPresent(u -> {
            throw new EmailAlreadyExistsException(request.getEmail());
        });
        byLogin.filter(User::isVerified).ifPresent(u -> {
            throw new LoginAlreadyExistsException(request.getLogin());
        });

        // Если email и login принадлежат разным неверифицированным пользователям —
        // удаляем того, кто занимает запрошенный login, чтобы освободить слот
        if (byEmail.isPresent() && byLogin.isPresent() && !byEmail.get().getId().equals(byLogin.get().getId())) {
            User staleLoginUser = byLogin.get();
            emailTokenService.deleteAllByUser(staleLoginUser);
            userRepository.delete(staleLoginUser);
        }

        // Приоритет: пользователь по email > пользователь по login
        Optional<User> existing = byEmail.isPresent() ? byEmail : byLogin;

        final User user;
        final boolean isNewUser;

        if (existing.isPresent()) {
            // Обновляем данные неверифицированного пользователя
            user = existing.get();
            user.setLogin(request.getLogin());
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            userRepository.save(user);
            isNewUser = false;
        } else {
            // Создаём нового пользователя
            Role userRole = roleRepository
                .findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RoleNotFoundException(RoleName.ROLE_USER.name()));

            String passwordHash = passwordEncoder.encode(request.getPassword());
            User newUser = userMapper.toEntity(request, passwordHash);
            newUser.getRoles().add(userRole);
            user = userRepository.save(newUser);
            isNewUser = true;
        }

        userEmailService.verifyEmail(user);

        if (isNewUser) userEventProducer.publishUserRegistered(user);

        return userMapper.toRegisterResponseDto(user);
    }

    @Transactional
    public void acceptVerification(UUID token) {
        EmailToken emailToken = emailTokenService.getValidToken(token);
        emailTokenService.markAsUsed(emailToken);

        User user = emailToken.getUser();
        user.setVerified(true);
        userRepository.save(user);
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
