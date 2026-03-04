package net.mocknet.user_service.common.factory;

import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Компонент для создания уникальных тестовых email токенов.
 */
public final class TestEmailTokenFactory {
    
    // Базовые значения для генерации уникальных данных
    private static final EmailTokenType BASE_TYPE = EmailTokenType.VERIFICATION;
    private static final int TOKEN_EXPIRY_HOURS = 24;

    private TestEmailTokenFactory() {
        // Приватный конструктор для предотвращения инстанциирования
    }

    /**
     * Создает новый email токен с уникальными данными для указанного пользователя
     */
    public static EmailToken createEmailToken(User user) {
        EmailToken emailToken = new EmailToken();
        emailToken.setId(UUID.randomUUID());
        emailToken.setToken(UUID.randomUUID());
        emailToken.setUser(user);
        emailToken.setType(BASE_TYPE);
        emailToken.setExpiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS).truncatedTo(ChronoUnit.MILLIS));
        emailToken.setUsedAt(null);
        
        return emailToken;
    }

    /**
     * Создает новый email токен с уникальными данными для указанного пользователя и типа
     */
    public static EmailToken createEmailToken(User user, EmailTokenType type) {
        EmailToken emailToken = new EmailToken();
        emailToken.setId(UUID.randomUUID());
        emailToken.setToken(UUID.randomUUID());
        emailToken.setUser(user);
        emailToken.setType(type);
        emailToken.setExpiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS).truncatedTo(ChronoUnit.MILLIS));
        emailToken.setUsedAt(null);
        
        return emailToken;
    }

    /**
     * Создает новый email токен с уникальными данными и кастомным сроком истечения
     */
    public static EmailToken createEmailToken(User user, EmailTokenType type, int expiryHours) {
        EmailToken emailToken = new EmailToken();
        emailToken.setId(UUID.randomUUID());
        emailToken.setToken(UUID.randomUUID());
        emailToken.setUser(user);
        emailToken.setType(type);
        emailToken.setExpiresAt(OffsetDateTime.now().plusHours(expiryHours).truncatedTo(ChronoUnit.MILLIS));
        emailToken.setUsedAt(null);
        
        return emailToken;
    }

    /**
     * Создает новый использованный email токен
     */
    public static EmailToken createUsedEmailToken(User user) {
        EmailToken emailToken = new EmailToken();
        emailToken.setId(UUID.randomUUID());
        emailToken.setToken(UUID.randomUUID());
        emailToken.setUser(user);
        emailToken.setType(BASE_TYPE);
        emailToken.setExpiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS).truncatedTo(ChronoUnit.MILLIS));
        emailToken.setUsedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        
        return emailToken;
    }

    /**
     * Создает новый просроченный email токен
     */
    public static EmailToken createExpiredEmailToken(User user) {
        EmailToken emailToken = new EmailToken();
        emailToken.setId(UUID.randomUUID());
        emailToken.setToken(UUID.randomUUID());
        emailToken.setUser(user);
        emailToken.setType(BASE_TYPE);
        emailToken.setExpiresAt(OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS));
        emailToken.setUsedAt(null);
        
        return emailToken;
    }
}