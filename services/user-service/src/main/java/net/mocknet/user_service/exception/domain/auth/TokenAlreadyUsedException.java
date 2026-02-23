package net.mocknet.user_service.exception.domain.auth;

import net.mocknet.user_service.exception.base.ConflictException;

import java.util.UUID;

public class TokenAlreadyUsedException extends ConflictException {

    public TokenAlreadyUsedException(UUID token) {
        super("exception.token.already-used", token);
    }
}
