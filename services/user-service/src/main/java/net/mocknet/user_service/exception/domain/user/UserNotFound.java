package net.mocknet.user_service.exception.domain.user;

import net.mocknet.user_service.exception.base.NotFoundException;

import java.util.UUID;

public class UserNotFound extends NotFoundException {

    public UserNotFound(UUID id) {
        super("exception.user.not-found", id);
    }
}
