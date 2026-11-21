package com.smartappointment.appointmentservice.messaging;

public record NotificationEmailDto(
        String destinataire,
        String objet,
        String contenu) {
}
