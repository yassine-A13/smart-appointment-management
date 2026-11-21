package com.smartappointment.notificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitListenerConfig.class);

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    @Bean
    public Advice retryInterceptor() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Échec du traitement du message RabbitMQ, tentative {} sur 3",
                        context.getRetryCount(), throwable);
            }
        });

        MessageRecoverer recoverer = (message, cause) -> {
            log.error("Échec définitif du traitement après 3 tentatives; le message sera rejeté sans requeue",
                    cause);
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
                    "Échec du traitement après 3 tentatives", cause);
        };

        return RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer(recoverer)
                .build();
    }
}
