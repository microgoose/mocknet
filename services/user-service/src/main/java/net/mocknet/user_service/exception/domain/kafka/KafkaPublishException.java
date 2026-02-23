package net.mocknet.user_service.exception.domain.kafka;

import net.mocknet.user_service.exception.base.InternalException;

public class KafkaPublishException extends InternalException {

    public KafkaPublishException(String eventType, Throwable cause) {
        super("exception.kafka.publish-failed", eventType);
        initCause(cause);
    }
}
