package com.finpay.notification.service.infrastructure.messaging;

import com.finpay.notification.service.domain.Notification;
import com.finpay.notification.service.domain.NotificationProcessor;
import com.finpay.notification.service.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes NotificationRequested (finpay.notification) and drives the
 * NotificationProcessor. Idempotent by eventId (Rule 7); dead-letters to
 * finpay.notification.dlq after the processor exhausts retries (FP-19/48).
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final NotificationProcessor processor;
    private final NotificationRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final String dlqTopic;
    private final Map<String, Boolean> seen = new ConcurrentHashMap<>();

    public NotificationConsumer(NotificationProcessor processor, NotificationRepository repository,
                                KafkaTemplate<String, String> kafka,
                                @org.springframework.beans.factory.annotation.Value("${finpay.notification.topics.dead-letter:finpay.notification.dlq}") String dlqTopic) {
        this.processor = processor;
        this.repository = repository;
        this.kafka = kafka;
        this.dlqTopic = dlqTopic;
    }

    @KafkaListener(topics = "finpay.notification", groupId = "notification-consumer")
    public void onEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record,
                        Acknowledgment ack) {
        String eventId = record.key();
        if (eventId != null && seen.putIfAbsent(eventId, Boolean.TRUE) != null) {
            log.info("dup event {} skipped", eventId);
            ack.acknowledge();
            return;
        }
        Map<String, Object> evt = parse(record.value());
        String notificationId = UUID.randomUUID().toString();
        try {
            processor.handleRequested(notificationId, eventId,
                    String.valueOf(evt.getOrDefault("customerId", "")),
                    channelOf(evt), String.valueOf(evt.getOrDefault("eventType", "Notification")),
                    record.value());
        } catch (Exception ex) {
            log.error("notification processing failed {}: {}", eventId, ex.getMessage());
        }
        // Dead-letter if the processor exhausted retries.
        repository.findByEventId(eventId)
                .filter(n -> n.state() == Notification.State.DEAD_LETTER)
                .ifPresent(n -> kafka.send(dlqTopic, eventId, record.value()));
        ack.acknowledge();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Notification.Channel channelOf(Map<String, Object> evt) {
        Object c = evt.getOrDefault("channel", "EMAIL");
        try { return Notification.Channel.valueOf(String.valueOf(c).toUpperCase()); }
        catch (Exception e) { return Notification.Channel.EMAIL; }
    }
}
