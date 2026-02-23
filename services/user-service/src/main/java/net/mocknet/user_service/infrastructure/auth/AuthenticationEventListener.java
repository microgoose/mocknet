package net.mocknet.user_service.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.model.security.SecurityUser;
import net.mocknet.user_service.service.user.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final UserService userService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof OAuth2AuthorizationCodeAuthenticationToken token) {
            if (token.getPrincipal() instanceof SecurityUser securityUser) {
                userService.updateLastLogin(securityUser.getId(), OffsetDateTime.now());
            }
        }
    }
}