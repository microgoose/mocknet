package net.mocknet.user_service.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "app.rsa")
public record RsaKeyProperties(
    String keyId,
    RSAPublicKey publicKey,
    RSAPrivateKey privateKey
) {
}