package net.mocknet.user_service.config;

import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.exception.LocalizedException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    public String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    public String msg(LocalizedException ex, Locale locale) {
        return messageSource.getMessage(
            ex.getMessageKey(),
            ex.getArgs(),
            ex.getMessageKey(),
            locale
        );
    }
}