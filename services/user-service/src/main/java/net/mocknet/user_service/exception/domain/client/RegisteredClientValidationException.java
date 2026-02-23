package net.mocknet.user_service.exception.domain.client;

import net.mocknet.user_service.exception.base.ValidationException;

public class RegisteredClientValidationException extends ValidationException {

    public RegisteredClientValidationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}