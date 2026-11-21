package com.smartappointment.notificationservice.service;

import com.smartappointment.notificationservice.dto.NotificationEmailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String DEVELOPMENT_SENDER = "notifications@smart-appointment.local";

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void envoyerEmail(NotificationEmailDto notification) {
        logger.info("Tentative d'envoi d'un email de notification");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(DEVELOPMENT_SENDER);
        message.setTo(notification.destinataire());
        message.setSubject(notification.objet());
        message.setText(notification.contenu());

        mailSender.send(message);
        logger.info("Email de notification envoyé avec succès");
    }
}
