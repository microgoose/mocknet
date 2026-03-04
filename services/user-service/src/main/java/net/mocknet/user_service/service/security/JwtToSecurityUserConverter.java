package net.mocknet.user_service.service.security;

import net.mocknet.user_service.model.security.SecurityUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtToSecurityUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(UUID.fromString(jwt.getClaim("user_id")));
        securityUser.setUsername(jwt.getSubject());
        securityUser.setEmail(jwt.getClaim("email"));
        securityUser.setPassword(null);
        securityUser.setEnabled(true);
        securityUser.setAuthorities(
            jwt.getClaimAsStringList("roles").stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList())
        );

        return new JwtAuthenticationToken(jwt, securityUser.getAuthorities(), securityUser.getUsername()) {
            @Override
            public Object getPrincipal() {
                return securityUser;
            }
        };
    }
}