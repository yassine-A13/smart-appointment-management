package com.smartappointment.notificationservice.messaging;

import com.smartappointment.notificationservice.config.RabbitConfig;
import com.smartappointment.notificationservice.dto.NotificationEmailDto;
import com.smartappointment.notificationservice.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final NotificationService notificationService;
    private final Validator validator;

    public NotificationConsumer(NotificationService notificationService, Validator validator) {
        this.notificationService = notificationService;
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_EMAIL)
    public void consommer(NotificationEmailDto notification) {
        log.info("Message de notification reçu");
        log.info("Début du traitement de la notification email");

        Set<ConstraintViolation<NotificationEmailDto>> violations = validator.validate(notification);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("message invalide");
            throw new IllegalArgumentException("Notification email invalide : " + details);
        }

        try {
            notificationService.envoyerEmail(notification);
            log.info("Email de notification envoyé avec succès");
        } catch (RuntimeException exception) {
            log.error("Échec du traitement de la notification email", exception);
            throw exception;
        }
    }
}
