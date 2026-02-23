package net.mocknet.user_service.exception;

import lombok.Getter;

@Getter
public abstract class LocalizedException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    protected LocalizedException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

}
