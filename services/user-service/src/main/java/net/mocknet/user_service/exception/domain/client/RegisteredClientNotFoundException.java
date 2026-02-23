package net.mocknet.user_service.exception.domain.client;

import net.mocknet.user_service.exception.base.NotFoundException;

public class RegisteredClientNotFoundException extends NotFoundException {

    public RegisteredClientNotFoundException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}