package net.mocknet.user_service.exception.base;

import net.mocknet.user_service.exception.LocalizedException;

public abstract class ValidationException extends LocalizedException {

    protected ValidationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}