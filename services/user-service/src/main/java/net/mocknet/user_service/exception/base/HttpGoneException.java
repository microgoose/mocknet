package net.mocknet.user_service.exception.base;

import net.mocknet.user_service.exception.LocalizedException;

public abstract class HttpGoneException extends LocalizedException {

    protected HttpGoneException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
