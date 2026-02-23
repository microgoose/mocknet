package net.mocknet.user_service.exception.domain.auth;

import net.mocknet.user_service.exception.base.HttpGoneException;

import java.util.UUID;

public class TokenExpiredException extends HttpGoneException {

    public TokenExpiredException(UUID token) {
        super("exception.token.expired", token);
    }
}
