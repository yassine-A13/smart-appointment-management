package com.smartappointment.appointmentservice.messaging;

import com.smartappointment.appointmentservice.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        rabbitTemplate.setConfirmCallback(this::onConfirm);
        rabbitTemplate.setReturnsCallback(this::onReturn);
        this.rabbitTemplate = rabbitTemplate;
    }

    private final RabbitTemplate rabbitTemplate;

    public void envoyer(NotificationEmailDto notification) {
        log.info("Publication d'une notification email vers la queue {}", RabbitConfig.EMAIL_NOTIFICATIONS_QUEUE);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_NOTIFICATIONS_QUEUE, notification);
            log.debug("Message de notification transmis à RabbitMQ, attente de confirmation broker");
        } catch (AmqpException exception) {
            log.error("Échec de publication de la notification email", exception);
            throw exception;
        }
    }

    private void onConfirm(CorrelationData correlationData, boolean acknowledged, String cause) {
        if (acknowledged) {
            log.info("RabbitMQ a confirmé la publication de la notification email");
        } else {
            log.error("RabbitMQ a refusé la publication de la notification email : {}", cause);
        }
    }

    private void onReturn(ReturnedMessage returned) {
        log.error("RabbitMQ n'a pas routé le message (code {}, {}) vers {} via la clé {}",
                returned.getReplyCode(), returned.getReplyText(), returned.getExchange(), returned.getRoutingKey());
    }
}
