package net.mocknet.user_service.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtils {
    
    // 1. Генерация code_verifier (случайная строка)
    public static String generateCodeVerifier() {
        // Требования: минимум 43 символа, максимум 128, только [a-zA-Z0-9\-._~]
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[32]; // 32 байта = 256 бит энтропии
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    // 2. Генерация code_challenge методом S256
    public static String generateCodeChallengeS256(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}