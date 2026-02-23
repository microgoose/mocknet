package net.mocknet.user_service.service.user;

import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.config.EmailVerificationConfig;
import net.mocknet.user_service.infrastructure.user.UserEventProducer;
import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.service.email.EmailTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserEmailService {

    private final EmailTokenService emailTokenService;
    private final EmailVerificationConfig emailVerificationConfig;
    private final UserEventProducer userEventProducer;

    @Transactional
    public void verifyEmail(User user) {
        emailTokenService.invalidateByUserAndType(user, EmailTokenType.VERIFICATION);
        EmailToken verificationToken = emailTokenService.createVerificationToken(user);

        userEventProducer.publishSendEmailVerification(
            user,
            verificationToken.getToken().toString(),
            verificationToken.getExpiresAt(),
            emailVerificationConfig.getDefaultLocale()
        );
    }
}
