package net.mocknet.user_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Data
@Configuration
@ConfigurationProperties("app.email-verification")
public class EmailVerificationConfig {
    private int tokenTtlHours;
    private Locale defaultLocale;
}
