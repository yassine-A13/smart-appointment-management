package com.smartappointment.appointmentservice.appointment;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentDTO> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AppointmentDTO request) {
        AppointmentDTO created = appointmentService.create(request, idempotencyKey);
        return ResponseEntity.created(URI.create("/appointments/" + created.id())).body(created);
    }

    @GetMapping
    public List<AppointmentDTO> findAll() {
        return appointmentService.findAll();
    }

    @GetMapping("/{id}")
    public AppointmentDTO findById(@PathVariable("id") Long id) {
        return appointmentService.findById(id);
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<AppointmentDTOFull> getAppointmentFullById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentFullById(id));
    }

    @PutMapping("/{id}")
    public AppointmentDTO update(@PathVariable("id") Long id, @Valid @RequestBody AppointmentDTO request) {
        return appointmentService.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentDTO cancel(@PathVariable("id") Long id) {
        return appointmentService.cancel(id);
    }

    @GetMapping("/client/{clientId}")
    public List<AppointmentDTO> findByClientId(@PathVariable("clientId") Long clientId) {
        return appointmentService.findByClientId(clientId);
    }
}
