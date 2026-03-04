package net.mocknet.user_service.common.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import net.mocknet.user_service.model.security.SecurityUser;
import net.mocknet.user_service.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class TokenGenerator {

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    public String generateBearerToken(SecurityUser user) {
        JWKSet jwkSet = ((ImmutableJWKSet<SecurityContext>) jwkSource).getJWKSet();
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId(rsaKey.getKeyID())
            .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(user.getUsername())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("user_id", user.getId())
            .claim("email", user.getEmail())
            .claim("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .build();

        return new NimbusJwtEncoder(jwkSource)
            .encode(JwtEncoderParameters.from(header, claims))
            .getTokenValue();
    }

    public String generateBearerToken(User user) {
        return generateBearerToken(new SecurityUser(user));
    }
}