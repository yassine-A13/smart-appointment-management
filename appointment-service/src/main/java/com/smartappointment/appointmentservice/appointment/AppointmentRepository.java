package com.smartappointment.appointmentservice.appointment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByClientId(Long clientId);

    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
}
