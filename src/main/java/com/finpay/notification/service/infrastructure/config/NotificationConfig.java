package com.finpay.notification.service.infrastructure.config;

import com.finpay.notification.service.domain.ChannelDispatcher;
import com.finpay.notification.service.domain.MessageGenerator;
import com.finpay.notification.service.domain.NotificationProcessor;
import com.finpay.notification.service.domain.NotificationRepository;
import com.finpay.notification.service.infrastructure.dispatch.MultiChannelDispatcher;
import com.finpay.notification.service.infrastructure.generator.HttpMessageGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public NotificationProcessor notificationProcessor(NotificationRepository repository,
                                                        MessageGenerator generator,
                                                        ChannelDispatcher dispatcher) {
        return new NotificationProcessor(repository, generator, dispatcher);
    }
}
