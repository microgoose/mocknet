package net.mocknet.user_service.exception.domain.user;

import net.mocknet.user_service.exception.base.ForbiddenException;

import java.util.UUID;

public class UserUpdateForbidden extends ForbiddenException {

    public UserUpdateForbidden(UUID id) {
        super("exception.user.update-forbidden", id);
    }
}
