package com.smartappointment.appointmentservice.appointment;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public AppointmentDTO create(AppointmentDTO request) {
        Appointment appointment = new Appointment(request.clientId(), request.serviceId(),
                request.appointmentDate(), request.status(), request.notes());
        return AppointmentDTO.from(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findAll() {
        return appointmentRepository.findAll().stream().map(AppointmentDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDTO findById(Long id) {
        return AppointmentDTO.from(getAppointment(id));
    }

    public AppointmentDTO update(Long id, AppointmentDTO request) {
        Appointment appointment = getAppointment(id);
        appointment.setClientId(request.clientId());
        appointment.setServiceId(request.serviceId());
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStatus(request.status());
        appointment.setNotes(request.notes());
        return AppointmentDTO.from(appointmentRepository.save(appointment));
    }

    public AppointmentDTO cancel(Long id) {
        Appointment appointment = getAppointment(id);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return AppointmentDTO.from(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByClientId(Long clientId) {
        return appointmentRepository.findByClientId(clientId).stream().map(AppointmentDTO::from).toList();
    }

    private Appointment getAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }
}
