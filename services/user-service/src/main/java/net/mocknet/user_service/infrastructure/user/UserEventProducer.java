package net.mocknet.user_service.infrastructure.user;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.contracts.EnvelopeFactory;
import net.mocknet.contracts.EventType;
import net.mocknet.contracts.Topics;
import net.mocknet.contracts.common.v1.EnvelopeProto.Envelope;
import net.mocknet.contracts.notification.v1.NotificationCommandsProto.SendEmailVerificationCommand;
import net.mocknet.contracts.user.v1.UserEventsProto.UserRegisteredEvent;
import net.mocknet.user_service.config.KafkaClient;
import net.mocknet.user_service.model.user.User;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaClient kafkaClient;

    public void publishUserRegistered(User user) {
        String userId = user.getId().toString();

        UserRegisteredEvent event = UserRegisteredEvent.newBuilder()
            .setUserId(userId)
            .setEmail(user.getEmail())
            .setLogin(user.getLogin())
            .setFirstName(
                user.getFirstName() != null ? user.getFirstName() : ""
            )
            .setLastName(user.getLastName() != null ? user.getLastName() : "")
            .build();

        Envelope envelope = EnvelopeFactory.wrap(
            EventType.USER_REGISTERED,
            event
        );
        kafkaClient.send(
            Topics.USER_EVENTS,
            userId,
            envelope,
            EventType.USER_REGISTERED
        );
    }

    public void publishSendEmailVerification(
        User user,
        String confirmationToken,
        OffsetDateTime expiresAt,
        String locale
    ) {
        String userId = user.getId().toString();
        String displayName =
            user.getFirstName() != null ? user.getFirstName() : user.getLogin();

        Timestamp expiresAtTimestamp = Timestamp.newBuilder()
            .setSeconds(expiresAt.toEpochSecond())
            .setNanos(expiresAt.getNano())
            .build();

        SendEmailVerificationCommand command =
            SendEmailVerificationCommand.newBuilder()
                .setUserId(userId)
                .setEmail(user.getEmail())
                .setName(displayName)
                .setLocale(locale)
                .setConfirmationToken(confirmationToken)
                .setExpiresAt(expiresAtTimestamp)
                .build();

        Envelope envelope = EnvelopeFactory.wrap(
            EventType.SEND_EMAIL_VERIFICATION,
            command
        );
        kafkaClient.send(
            Topics.NOTIFICATION_COMMANDS,
            userId,
            envelope,
            EventType.SEND_EMAIL_VERIFICATION
        );
    }
}
