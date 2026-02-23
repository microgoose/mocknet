package net.mocknet.user_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.contracts.EventType;
import net.mocknet.contracts.common.v1.EnvelopeProto;
import net.mocknet.user_service.exception.domain.kafka.KafkaPublishException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaClient {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public void send(
        String topic,
        String key,
        EnvelopeProto.Envelope envelope,
        EventType eventType
    ) {
        byte[] payload = envelope.toByteArray();
        log.debug(
            "Публикуем Kafka событие [{}] в топик '{}', ключ: '{}'",
            eventType,
            topic,
            key
        );

        try {
            SendResult<String, byte[]> result = kafkaTemplate
                .send(topic, key, payload)
                .get();
            log.debug(
                "Опубликовано [{}], partition: {}, offset: {}",
                eventType,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException(eventType.name(), e);
        } catch (ExecutionException e) {
            throw new KafkaPublishException(
                eventType.name(),
                e.getCause() != null ? e.getCause() : e
            );
        } catch (Exception e) {
            throw new KafkaPublishException(eventType.name(), e);
        }
    }
}
