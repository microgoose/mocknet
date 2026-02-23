package net.mocknet.user_service.service.email;

import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.config.EmailVerificationConfig;
import net.mocknet.user_service.exception.domain.auth.TokenAlreadyUsedException;
import net.mocknet.user_service.exception.domain.auth.TokenExpiredException;
import net.mocknet.user_service.exception.domain.auth.TokenNotFoundException;
import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.EmailTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private final EmailTokenRepository emailTokenRepository;
    private final EmailVerificationConfig emailVerificationConfig;

    public EmailToken getValidToken(UUID token) {
        EmailToken emailToken = emailTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new TokenNotFoundException(token));

        if (emailToken.isUsed()) {
            throw new TokenAlreadyUsedException(token);
        }

        if (emailToken.isExpired()) {
            throw new TokenExpiredException(token);
        }

        return emailToken;
    }

    @Transactional
    public EmailToken createVerificationToken(User user) {
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID());
        token.setType(EmailTokenType.VERIFICATION);
        token.setExpiresAt(
            OffsetDateTime.now().plusHours(
                emailVerificationConfig.getTokenTtlHours()
            )
        );
        return emailTokenRepository.save(token);
    }

    @Transactional
    public void markAsUsed(EmailToken token) {
        token.setUsedAt(OffsetDateTime.now());
        emailTokenRepository.save(token);
    }

    @Transactional
    public void invalidateByUserAndType(User user, EmailTokenType type) {
        List<EmailToken> tokens = emailTokenRepository.findAllByUserAndType(user, type);
        if (tokens.isEmpty()) return;
        OffsetDateTime now = OffsetDateTime.now();
        tokens.forEach(t -> t.setUsedAt(now));
        emailTokenRepository.saveAll(tokens);
    }

    @Transactional
    public void deleteAllByUser(User user) {
        List<EmailToken> tokens = emailTokenRepository.findAllByUser(user);
        if (!tokens.isEmpty()) {
            emailTokenRepository.deleteAll(tokens);
        }
    }
}