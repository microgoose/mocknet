package net.mocknet.contracts;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Timestamp;
import net.mocknet.contracts.common.v1.EnvelopeProto.Envelope;

import java.time.Instant;
import java.util.UUID;

public final class EnvelopeFactory {

    private EnvelopeFactory() {}

    public static Envelope wrap(EventType eventType, MessageLite payload) {
        Instant now = Instant.now();
        return Envelope.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventType(eventType.name())
            .setOccurredAt(
                Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build()
            )
            .setPayload(payload.toByteString())
            .build();
    }
}
