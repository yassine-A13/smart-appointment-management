package com.smartappointment.notificationservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String EMAIL_NOTIFICATIONS_QUEUE = "notifications.email";
    public static final String QUEUE_EMAIL = EMAIL_NOTIFICATIONS_QUEUE;

    @Bean
    public Queue emailNotificationsQueue() {
        return QueueBuilder.durable(EMAIL_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages(
                "com.smartappointment.appointmentservice.messaging",
                "com.smartappointment.notificationservice.dto");
        classMapper.setIdClassMapping(Map.of(
                "com.smartappointment.appointmentservice.messaging.NotificationEmailDto",
                com.smartappointment.notificationservice.dto.NotificationEmailDto.class));
        converter.setClassMapper(classMapper);
        return converter;
    }
}
