package unit.service.user;

import common.TestEmailTokenFactory;
import net.mocknet.user_service.config.EmailVerificationConfig;
import net.mocknet.user_service.infrastructure.user.UserEventProducer;
import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.service.email.EmailTokenService;
import net.mocknet.user_service.service.user.UserEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;


import static common.TestEmailTokenFactory.createBaseEmailToken;
import static common.TestUserFactory.createBaseUser;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEmailServiceTest {

    @Mock
    private EmailTokenService emailTokenService;

    @Mock
    private EmailVerificationConfig emailVerificationConfig;

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserEmailService userEmailService;

    private User testUser;
    private EmailToken testVerificationToken;
    private UUID testTokenUuid;
    private OffsetDateTime testExpiresAt;
    private Locale testLocale;
    private String testLocaleString;

    @BeforeEach
    void setUp() {
        testUser = createBaseUser();
        testUser.setVerified(false);

        testTokenUuid = TestEmailTokenFactory.TOKEN;
        testExpiresAt = TestEmailTokenFactory.EXPIRES_AT;
        testLocale = Locale.US;
        testLocaleString = testLocale.toString();

        testVerificationToken = createBaseEmailToken();
    }

    @Test
    void verifyEmail_Success_ShouldInvalidateOldTokensAndCreateNewAndPublishEvent() {
        // Arrange
        when(emailVerificationConfig.getDefaultLocale()).thenReturn(testLocale);
        when(emailTokenService.createVerificationToken(testUser)).thenReturn(testVerificationToken);

        // Act
        userEmailService.verifyEmail(testUser);

        // Assert
        // Verify that old tokens are invalidated
        verify(emailTokenService, times(1))
            .invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);

        // Verify that new token is created
        verify(emailTokenService, times(1))
            .createVerificationToken(testUser);

        // Verify that event is published with correct parameters
        verify(userEventProducer, times(1))
            .publishSendEmailVerification(
                eq(testUser),
                eq(testTokenUuid.toString()),
                eq(testExpiresAt),
                eq(testLocaleString)
            );
    }

    @Test
    void verifyEmail_ShouldInvalidateOldTokensBeforeCreatingNewOne() {
        // Arrange
        when(emailVerificationConfig.getDefaultLocale()).thenReturn(testLocale);
        when(emailTokenService.createVerificationToken(testUser)).thenReturn(testVerificationToken);

        // Act
        userEmailService.verifyEmail(testUser);

        // Assert - verify order of operations
        InOrder inOrder = inOrder(emailTokenService, userEventProducer);

        inOrder.verify(emailTokenService).invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);
        inOrder.verify(emailTokenService).createVerificationToken(testUser);
        inOrder.verify(userEventProducer).publishSendEmailVerification(
            eq(testUser),
            eq(testTokenUuid.toString()),
            eq(testExpiresAt),
            eq(testLocaleString)
        );
    }

    @Test
    void verifyEmail_WithDifferentLocale_ShouldUseConfiguredLocale() {
        // Arrange
        Locale customLocale = Locale.GERMANY;
        when(emailVerificationConfig.getDefaultLocale()).thenReturn(customLocale);
        when(emailTokenService.createVerificationToken(testUser)).thenReturn(testVerificationToken);

        // Act
        userEmailService.verifyEmail(testUser);

        // Assert
        verify(userEventProducer).publishSendEmailVerification(
            eq(testUser),
            eq(testTokenUuid.toString()),
            eq(testExpiresAt),
            eq(customLocale.toString())
        );
    }

    @Test
    void verifyEmail_WhenTokenCreationReturnsToken_ShouldPublishEventWithCorrectTokenData() {
        // Arrange
        UUID expectedTokenUuid = UUID.randomUUID();
        OffsetDateTime expectedExpiresAt = OffsetDateTime.now().plusHours(48);

        EmailToken customToken = createBaseEmailToken();
        customToken.setToken(expectedTokenUuid);
        customToken.setExpiresAt(expectedExpiresAt);

        when(emailVerificationConfig.getDefaultLocale()).thenReturn(testLocale);
        when(emailTokenService.createVerificationToken(testUser)).thenReturn(customToken);

        // Act
        userEmailService.verifyEmail(testUser);

        // Assert
        verify(userEventProducer).publishSendEmailVerification(
            eq(testUser),
            eq(expectedTokenUuid.toString()),
            eq(expectedExpiresAt),
            eq(testLocale.toString())
        );
    }

    @Test
    void verifyEmail_WhenCalledMultipleTimes_ShouldInvalidatePreviousTokensEachTime() {
        // Arrange
        when(emailVerificationConfig.getDefaultLocale()).thenReturn(testLocale);
        when(emailTokenService.createVerificationToken(testUser))
            .thenReturn(testVerificationToken)
            .thenReturn(testVerificationToken);

        // Act
        userEmailService.verifyEmail(testUser);
        userEmailService.verifyEmail(testUser);

        // Assert
        verify(emailTokenService, times(2))
            .invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);
        verify(emailTokenService, times(2))
            .createVerificationToken(testUser);
        verify(userEventProducer, times(2))
            .publishSendEmailVerification(
                eq(testUser),
                eq(testTokenUuid.toString()),
                eq(testExpiresAt),
                eq(testLocale.toString())
            );
    }
}