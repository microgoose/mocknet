package common;

import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class TestEmailTokenFactory {

    public static final UUID ID = UUID.randomUUID();
    public static final UUID TOKEN = UUID.randomUUID();
    public static final User USER = TestUserFactory.createBaseUser();
    public static final EmailTokenType TYPE = EmailTokenType.VERIFICATION;
    public static final OffsetDateTime EXPIRES_AT = OffsetDateTime.now().plusHours(24).truncatedTo(ChronoUnit.MILLIS);
    public static final OffsetDateTime USED_AT = null;

    public static EmailToken createBaseEmailToken(User user) {
        EmailToken emailToken = new EmailToken();
        emailToken.setId(ID);
        emailToken.setToken(TOKEN);
        emailToken.setUser(user);
        emailToken.setType(TYPE);
        emailToken.setExpiresAt(EXPIRES_AT);
        emailToken.setUsedAt(USED_AT);
        return emailToken;
    }

    public static EmailToken createBaseEmailToken() {
        return createBaseEmailToken(USER);
    }
}