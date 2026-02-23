package net.mocknet.user_service.exception.domain.role;

import net.mocknet.user_service.exception.base.InternalException;

public class RoleNotFoundException extends InternalException {

    public RoleNotFoundException(String roleName) {
        super("exception.role.not-found", roleName);
    }
}
