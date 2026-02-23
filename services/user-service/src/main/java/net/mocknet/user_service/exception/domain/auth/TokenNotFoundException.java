package net.mocknet.user_service.exception.domain.auth;

import net.mocknet.user_service.exception.base.NotFoundException;

import java.util.UUID;

public class TokenNotFoundException extends NotFoundException {

    public TokenNotFoundException(UUID token) {
        super("exception.token.not-found", token);
    }
}
