package com.smartappointment.appointmentservice.messaging;

import com.smartappointment.appointmentservice.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEmailDtoJsonTest {

    @Test
    void convertsNotificationToJsonWithExpectedFields() {
        var converter = new RabbitConfig().rabbitMessageConverter();
        var notification = new NotificationEmailDto("client@example.com", "Rappel", "Votre rendez-vous est confirmé.");

        Message message = converter.toMessage(notification, new MessageProperties());
        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(message.getMessageProperties().getContentType().contains("json"));
        assertTrue(json.contains("\"destinataire\":\"client@example.com\""));
        assertTrue(json.contains("\"objet\":\"Rappel\""));
        assertTrue(json.contains("\"contenu\":\"Votre rendez-vous est confirmé.\""));
    }
}
