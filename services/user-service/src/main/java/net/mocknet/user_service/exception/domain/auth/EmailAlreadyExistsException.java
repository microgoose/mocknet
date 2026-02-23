package net.mocknet.user_service.exception.domain.auth;

import net.mocknet.user_service.exception.base.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {

    public EmailAlreadyExistsException(String email) {
        super("exception.email.already-exists", email);
    }
}
