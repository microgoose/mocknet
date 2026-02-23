package net.mocknet.user_service.exception.domain.auth;

import net.mocknet.user_service.exception.base.ConflictException;

public class LoginAlreadyExistsException extends ConflictException {

    public LoginAlreadyExistsException(String login) {
        super("exception.login.already-exists", login);
    }
}
