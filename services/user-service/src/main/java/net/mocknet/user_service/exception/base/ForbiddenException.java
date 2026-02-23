package net.mocknet.user_service.exception.base;

import net.mocknet.user_service.exception.LocalizedException;

public abstract class ForbiddenException extends LocalizedException {

    protected ForbiddenException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
