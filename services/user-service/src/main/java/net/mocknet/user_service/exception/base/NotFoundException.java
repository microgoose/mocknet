package net.mocknet.user_service.exception.base;

import net.mocknet.user_service.exception.LocalizedException;

public abstract class NotFoundException extends LocalizedException {

    protected NotFoundException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
