package com.smartappointment.appointmentservice.appointment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record AppointmentDTO(
        Long id,
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long serviceId,
        @NotNull @FutureOrPresent LocalDateTime appointmentDate,
        @NotNull AppointmentStatus status,
        String notes) {

    static AppointmentDTO from(Appointment appointment) {
        return new AppointmentDTO(appointment.getId(), appointment.getClientId(), appointment.getServiceId(),
                appointment.getAppointmentDate(), appointment.getStatus(), appointment.getNotes());
    }
}
