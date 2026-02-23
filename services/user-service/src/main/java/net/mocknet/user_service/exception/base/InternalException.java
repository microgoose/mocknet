package net.mocknet.user_service.exception.base;

import net.mocknet.user_service.exception.LocalizedException;

public abstract class InternalException extends LocalizedException {

    protected InternalException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
