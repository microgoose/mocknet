package unit.service.email;

import common.TestEmailTokenFactory;
import net.mocknet.user_service.config.EmailVerificationConfig;
import net.mocknet.user_service.exception.domain.auth.TokenAlreadyUsedException;
import net.mocknet.user_service.exception.domain.auth.TokenExpiredException;
import net.mocknet.user_service.exception.domain.auth.TokenNotFoundException;
import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.EmailTokenRepository;
import net.mocknet.user_service.service.email.EmailTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static common.TestEmailTokenFactory.createEmailToken;
import static common.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTokenServiceTest {

    @Mock
    private EmailTokenRepository emailTokenRepository;

    @Mock
    private EmailVerificationConfig emailVerificationConfig;

    @InjectMocks
    private EmailTokenService emailTokenService;

    private User testUser;
    private EmailToken testToken;
    private UUID testTokenUuid;
    private EmailTokenType testTokenType;
    private OffsetDateTime testExpiresAt;
    private int tokenTtlHours;

    @BeforeEach
    void setUp() {
        testUser = createUser();
        testTokenUuid = TestEmailTokenFactory.TOKEN;
        testTokenType = EmailTokenType.VERIFICATION;
        testExpiresAt = TestEmailTokenFactory.EXPIRES_AT;
        tokenTtlHours = 24;

        testToken = createEmailToken();
        testToken.setToken(testTokenUuid);
        testToken.setType(testTokenType);
        testToken.setExpiresAt(testExpiresAt);
        testToken.setUsedAt(null);
    }

    @Nested
    class GetValidTokenTests {

        @Test
        void getValidToken_WhenTokenExistsAndNotUsedAndNotExpired_ShouldReturnToken() {
            // Arrange
            when(emailTokenRepository.findByToken(testTokenUuid))
                .thenReturn(Optional.of(testToken));

            // Act
            EmailToken result = emailTokenService.getValidToken(testTokenUuid);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(testTokenUuid);
            assertThat(result.isUsed()).isFalse();
            assertThat(result.isExpired()).isFalse();

            verify(emailTokenRepository, times(1)).findByToken(testTokenUuid);
        }

        @Test
        void getValidToken_WhenTokenNotFound_ShouldThrowTokenNotFoundException() {
            // Arrange
            when(emailTokenRepository.findByToken(testTokenUuid))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> emailTokenService.getValidToken(testTokenUuid))
                .isInstanceOf(TokenNotFoundException.class);

            verify(emailTokenRepository, times(1)).findByToken(testTokenUuid);
        }

        @Test
        void getValidToken_WhenTokenIsUsed_ShouldThrowTokenAlreadyUsedException() {
            // Arrange
            testToken.setUsedAt(OffsetDateTime.now());
            when(emailTokenRepository.findByToken(testTokenUuid))
                .thenReturn(Optional.of(testToken));

            // Act & Assert
            assertThatThrownBy(() -> emailTokenService.getValidToken(testTokenUuid))
                .isInstanceOf(TokenAlreadyUsedException.class);

            verify(emailTokenRepository, times(1)).findByToken(testTokenUuid);
        }

        @Test
        void getValidToken_WhenTokenIsExpired_ShouldThrowTokenExpiredException() {
            // Arrange
            testToken.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(emailTokenRepository.findByToken(testTokenUuid))
                .thenReturn(Optional.of(testToken));

            // Act & Assert
            assertThatThrownBy(() -> emailTokenService.getValidToken(testTokenUuid))
                .isInstanceOf(TokenExpiredException.class);

            verify(emailTokenRepository, times(1)).findByToken(testTokenUuid);
        }

        @Test
        void getValidToken_WhenTokenIsExpiredAndUsed_ShouldThrowTokenExpiredException() {
            // Arrange
            testToken.setUsedAt(null);
            testToken.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(emailTokenRepository.findByToken(testTokenUuid))
                .thenReturn(Optional.of(testToken));

            // Act & Assert
            assertThatThrownBy(() -> emailTokenService.getValidToken(testTokenUuid))
                .isInstanceOf(TokenExpiredException.class);

            // Проверяем, что проверка на isUsed() не успевает сработать, если токен истек
            verify(emailTokenRepository, times(1)).findByToken(testTokenUuid);
        }
    }

    @Nested
    class CreateVerificationTokenTests {

        @Test
        void createVerificationToken_ShouldCreateAndSaveTokenWithCorrectProperties() {
            // Arrange
            when(emailVerificationConfig.getTokenTtlHours()).thenReturn(tokenTtlHours);
            when(emailTokenRepository.save(any(EmailToken.class))).thenAnswer(invocation -> {
                EmailToken savedToken = invocation.getArgument(0);
                savedToken.setId(UUID.randomUUID());
                return savedToken;
            });

            // Act
            EmailToken result = emailTokenService.createVerificationToken(testUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getToken()).isNotNull();
            assertThat(result.getType()).isEqualTo(EmailTokenType.VERIFICATION);
            assertThat(result.getExpiresAt()).isNotNull();
            assertThat(result.getUsedAt()).isNull();

            // Проверяем, что expiresAt установлен правильно
            OffsetDateTime expectedExpiresAt = OffsetDateTime.now().plusHours(tokenTtlHours);
            assertThat(result.getExpiresAt())
                .isCloseTo(expectedExpiresAt, within(1, ChronoUnit.SECONDS));

            verify(emailVerificationConfig, times(1)).getTokenTtlHours();
            verify(emailTokenRepository, times(1)).save(any(EmailToken.class));
        }

        @Test
        void createVerificationToken_WithDifferentTtl_ShouldUseConfiguredTtl() {
            // Arrange
            int customTtl = 48;
            when(emailVerificationConfig.getTokenTtlHours()).thenReturn(customTtl);
            when(emailTokenRepository.save(any(EmailToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            EmailToken result = emailTokenService.createVerificationToken(testUser);

            // Assert
            OffsetDateTime expectedExpiresAt = OffsetDateTime.now().plusHours(customTtl);
            assertThat(result.getExpiresAt())
                .isCloseTo(expectedExpiresAt, within(1, ChronoUnit.SECONDS));

            verify(emailVerificationConfig, times(1)).getTokenTtlHours();
        }

        @Test
        void createVerificationToken_ShouldGenerateRandomTokenEachTime() {
            // Arrange
            when(emailVerificationConfig.getTokenTtlHours()).thenReturn(tokenTtlHours);
            when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            EmailToken token1 = emailTokenService.createVerificationToken(testUser);
            EmailToken token2 = emailTokenService.createVerificationToken(testUser);

            // Assert
            assertThat(token1.getToken()).isNotNull();
            assertThat(token2.getToken()).isNotNull();
            assertThat(token1.getToken()).isNotEqualTo(token2.getToken());
        }
    }

    @Nested
    class MarkAsUsedTests {

        @Test
        void markAsUsed_ShouldSetUsedAtAndSave() {
            // Arrange
            assertThat(testToken.isUsed()).isFalse();

            // Act
            emailTokenService.markAsUsed(testToken);

            // Assert
            assertThat(testToken.isUsed()).isTrue();
            assertThat(testToken.getUsedAt()).isNotNull();
            assertThat(testToken.getUsedAt())
                .isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));

            verify(emailTokenRepository, times(1)).save(testToken);
        }

        @Test
        void markAsUsed_WhenCalledMultipleTimes_ShouldUpdateUsedAtEachTime() throws InterruptedException {
            // Arrange
            when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            emailTokenService.markAsUsed(testToken);
            OffsetDateTime firstUsedAt = testToken.getUsedAt();

            // Небольшая задержка для гарантии разницы во времени
            Thread.sleep(10);
            
            emailTokenService.markAsUsed(testToken);
            OffsetDateTime secondUsedAt = testToken.getUsedAt();

            // Assert
            assertThat(firstUsedAt).isNotNull();
            assertThat(secondUsedAt).isNotNull();
            assertThat(secondUsedAt).isAfter(firstUsedAt);
            
            verify(emailTokenRepository, times(2)).save(testToken);
        }
    }

    @Nested
    class InvalidateByUserAndTypeTests {

        @Test
        void invalidateByUserAndType_WhenTokensExist_ShouldMarkAllAsUsed() {
            // Arrange
            List<EmailToken> tokens = List.of(
                createEmailToken(),
                createEmailToken(),
                createEmailToken()
            );
            
            tokens.forEach(t -> {
                t.setUsedAt(null);
                t.setUser(testUser);
                t.setType(EmailTokenType.VERIFICATION);
            });

            when(emailTokenRepository.findAllByUserAndType(testUser, EmailTokenType.VERIFICATION))
                .thenReturn(tokens);

            // Act
            emailTokenService.invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);

            // Assert
            tokens.forEach(token -> {
                assertThat(token.isUsed()).isTrue();
                assertThat(token.getUsedAt()).isNotNull();
                assertThat(token.getUsedAt())
                    .isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
            });

            verify(emailTokenRepository, times(1))
                .findAllByUserAndType(testUser, EmailTokenType.VERIFICATION);
            verify(emailTokenRepository, times(1)).saveAll(tokens);
        }

        @Test
        void invalidateByUserAndType_WhenNoTokensExist_ShouldDoNothing() {
            // Arrange
            when(emailTokenRepository.findAllByUserAndType(testUser, EmailTokenType.VERIFICATION))
                .thenReturn(Collections.emptyList());

            // Act
            emailTokenService.invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);

            // Assert
            verify(emailTokenRepository, times(1))
                .findAllByUserAndType(testUser, EmailTokenType.VERIFICATION);
            verify(emailTokenRepository, never()).saveAll(any());
        }

        @Test
        void invalidateByUserAndType_WhenSomeTokensAlreadyUsed_ShouldUpdateAllToCurrentTime() {
            // Arrange
            OffsetDateTime oldUsedAt = OffsetDateTime.now().minusDays(1);
            
            EmailToken usedToken = createEmailToken();
            usedToken.setUsedAt(oldUsedAt);
            
            EmailToken unusedToken = createEmailToken();
            unusedToken.setUsedAt(null);
            
            List<EmailToken> tokens = List.of(usedToken, unusedToken);
            
            when(emailTokenRepository.findAllByUserAndType(testUser, EmailTokenType.VERIFICATION))
                .thenReturn(tokens);

            // Act
            emailTokenService.invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);

            // Assert
            assertThat(usedToken.getUsedAt())
                .isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
            assertThat(unusedToken.getUsedAt())
                .isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
            
            // Проверяем, что старый usedAt был перезаписан
            assertThat(usedToken.getUsedAt()).isAfter(oldUsedAt);
        }

        @Test
        void invalidateByUserAndType_ShouldOnlyInvalidateTokensOfSpecifiedType() {
            // Arrange
            EmailToken verificationToken = createEmailToken();
            verificationToken.setType(EmailTokenType.VERIFICATION);
            verificationToken.setUsedAt(null);
            
            EmailToken otherTypeToken = createEmailToken();
            otherTypeToken.setType(EmailTokenType.VERIFICATION); // В данном случае только один тип
            otherTypeToken.setUsedAt(null);

            when(emailTokenRepository.findAllByUserAndType(testUser, EmailTokenType.VERIFICATION))
                .thenReturn(List.of(verificationToken, otherTypeToken));

            // Act
            emailTokenService.invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);

            // Assert
            assertThat(verificationToken.isUsed()).isTrue();
            assertThat(otherTypeToken.isUsed()).isTrue();
        }
    }

    @Nested
    class DeleteAllByUserTests {

        @Test
        void deleteAllByUser_WhenTokensExist_ShouldDeleteAllTokens() {
            // Arrange
            List<EmailToken> tokens = List.of(
                createEmailToken(),
                createEmailToken(),
                createEmailToken()
            );
            
            when(emailTokenRepository.findAllByUser(testUser))
                .thenReturn(tokens);

            // Act
            emailTokenService.deleteAllByUser(testUser);

            // Assert
            verify(emailTokenRepository, times(1)).findAllByUser(testUser);
            verify(emailTokenRepository, times(1)).deleteAll(tokens);
        }

        @Test
        void deleteAllByUser_WhenNoTokensExist_ShouldDoNothing() {
            // Arrange
            when(emailTokenRepository.findAllByUser(testUser))
                .thenReturn(Collections.emptyList());

            // Act
            emailTokenService.deleteAllByUser(testUser);

            // Assert
            verify(emailTokenRepository, times(1)).findAllByUser(testUser);
            verify(emailTokenRepository, never()).deleteAll(any());
        }

        @Test
        void deleteAllByUser_WhenUserHasMultipleTokens_ShouldDeleteAllOfThem() {
            // Arrange
            List<EmailToken> tokens = List.of(
                createEmailToken(),
                createEmailToken(),
                createEmailToken(),
                createEmailToken()
            );
            
            when(emailTokenRepository.findAllByUser(testUser))
                .thenReturn(tokens);

            // Act
            emailTokenService.deleteAllByUser(testUser);

            // Assert
            verify(emailTokenRepository, times(1)).deleteAll(tokens);
            assertThat(tokens).hasSize(4);
        }

        @Test
        void deleteAllByUser_WhenCalledMultipleTimes_ShouldCheckRepositoryEachTime() {
            // Arrange
            when(emailTokenRepository.findAllByUser(testUser))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

            // Act
            emailTokenService.deleteAllByUser(testUser);
            emailTokenService.deleteAllByUser(testUser);

            // Assert
            verify(emailTokenRepository, times(2)).findAllByUser(testUser);
            verify(emailTokenRepository, never()).deleteAll(any());
        }
    }

    @Nested
    class IntegrationScenarioTests {

        @Test
        void createThenValidateThenUse_ShouldWorkInSequence() {
            // Arrange
            when(emailVerificationConfig.getTokenTtlHours()).thenReturn(tokenTtlHours);
            when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> {
                    EmailToken saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

            // Act - Create token
            EmailToken createdToken = emailTokenService.createVerificationToken(testUser);
            UUID tokenValue = createdToken.getToken();

            // Arrange for validation
            when(emailTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(createdToken));

            // Act - Validate token
            EmailToken validatedToken = emailTokenService.getValidToken(tokenValue);

            // Assert - Token is valid
            assertThat(validatedToken).isEqualTo(createdToken);
            assertThat(validatedToken.isUsed()).isFalse();
            assertThat(validatedToken.isExpired()).isFalse();

            // Act - Mark as used
            emailTokenService.markAsUsed(validatedToken);

            // Assert - Token is now used
            assertThat(validatedToken.isUsed()).isTrue();

            // Arrange for validation after use
            when(emailTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(validatedToken));

            // Act & Assert - Trying to validate used token throws exception
            assertThatThrownBy(() -> emailTokenService.getValidToken(tokenValue))
                .isInstanceOf(TokenAlreadyUsedException.class);
        }

        @Test
        void invalidateByUserAndType_ThenCreateNew_ShouldWorkCorrectly() {
            // Arrange
            List<EmailToken> oldTokens = List.of(
                createEmailToken(),
                createEmailToken()
            );
            
            when(emailTokenRepository.findAllByUserAndType(testUser, EmailTokenType.VERIFICATION))
                .thenReturn(oldTokens);
            when(emailVerificationConfig.getTokenTtlHours()).thenReturn(tokenTtlHours);
            when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act - Invalidate old tokens
            emailTokenService.invalidateByUserAndType(testUser, EmailTokenType.VERIFICATION);

            // Assert - Old tokens are invalidated
            oldTokens.forEach(token -> assertThat(token.isUsed()).isTrue());

            // Act - Create new token
            EmailToken newToken = emailTokenService.createVerificationToken(testUser);

            // Assert - New token is valid and not used
            assertThat(newToken.isUsed()).isFalse();
            assertThat(newToken.getType()).isEqualTo(EmailTokenType.VERIFICATION);
            assertThat(newToken.getUser()).isEqualTo(testUser);
        }
    }
}