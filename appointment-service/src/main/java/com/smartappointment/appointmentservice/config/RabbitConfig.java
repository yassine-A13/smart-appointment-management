package com.smartappointment.appointmentservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EMAIL_NOTIFICATIONS_QUEUE = "notifications.email";

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
        converter.setClassMapper(classMapper);
        return converter;
    }
}
