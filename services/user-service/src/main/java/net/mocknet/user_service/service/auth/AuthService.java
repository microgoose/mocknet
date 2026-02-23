package net.mocknet.user_service.service.auth;

import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.exception.domain.auth.EmailAlreadyExistsException;
import net.mocknet.user_service.exception.domain.auth.LoginAlreadyExistsException;
import net.mocknet.user_service.exception.domain.role.RoleNotFoundException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserEmailService userEmailService;
    private final EmailTokenService emailTokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        Optional<User> byEmail = userRepository.findByEmail(request.getEmail());
        Optional<User> byLogin = userRepository.findByLogin(request.getLogin());

        // Если найден верифицированный пользователь — конфликт
        byEmail
            .filter(User::isVerified)
            .ifPresent(u -> {
                throw new EmailAlreadyExistsException(request.getEmail());
            });
        byLogin
            .filter(User::isVerified)
            .ifPresent(u -> {
                throw new LoginAlreadyExistsException(request.getLogin());
            });

        // Если email и login принадлежат разным неверифицированным пользователям —
        // удаляем того, кто занимает запрошенный login, чтобы освободить слот
        if (
            byEmail.isPresent() &&
            byLogin.isPresent() &&
            !byEmail.get().getId().equals(byLogin.get().getId())
        ) {
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
                .orElseThrow(() ->
                    new RoleNotFoundException(RoleName.ROLE_USER.name())
                );

            String passwordHash = passwordEncoder.encode(request.getPassword());
            User newUser = userMapper.toEntity(request, passwordHash);
            newUser.getRoles().add(userRole);
            user = userRepository.save(newUser);
            isNewUser = true;
        }

        userEmailService.verifyEmail(user);

        if (isNewUser)
            userEventProducer.publishUserRegistered(user);

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
}
