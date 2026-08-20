package com.finpay.notification.service.infrastructure.config;

import com.finpay.notification.service.domain.ChannelDispatcher;
import com.finpay.notification.service.domain.MessageGenerator;
import com.finpay.notification.service.domain.NotificationProcessor;
import com.finpay.notification.service.domain.NotificationRepository;
import com.finpay.notification.service.infrastructure.dispatch.MultiChannelDispatcher;
import com.finpay.notification.service.infrastructure.generator.HttpMessageGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class NotificationConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka.finpay-infra.svc.cluster.local:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public NotificationProcessor notificationProcessor(NotificationRepository repository,
                                                        MessageGenerator generator,
                                                        ChannelDispatcher dispatcher) {
        return new NotificationProcessor(repository, generator, dispatcher);
    }
}
