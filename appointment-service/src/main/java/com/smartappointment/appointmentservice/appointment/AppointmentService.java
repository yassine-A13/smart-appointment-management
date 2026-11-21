package com.smartappointment.appointmentservice.appointment;

import com.smartappointment.appointmentservice.feignClient.ClientsRequest;
import com.smartappointment.appointmentservice.feignClient.ClientDto;
import com.smartappointment.appointmentservice.feignClient.ServiceDto;
import com.smartappointment.appointmentservice.feignClient.ServiceRequest;
import com.smartappointment.appointmentservice.messaging.NotificationEmailDto;
import com.smartappointment.appointmentservice.messaging.NotificationPublisher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final ClientsRequest clientsRequest;
    private final ServiceRequest serviceRequest;
    private final AppointmentMapperFull appointmentMapperFull;
    private final NotificationPublisher notificationPublisher;

    public AppointmentService(AppointmentRepository appointmentRepository, ClientsRequest clientsRequest,
            ServiceRequest serviceRequest, AppointmentMapperFull appointmentMapperFull,
            NotificationPublisher notificationPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.clientsRequest = clientsRequest;
        this.serviceRequest = serviceRequest;
        this.appointmentMapperFull = appointmentMapperFull;
        this.notificationPublisher = notificationPublisher;
    }

    public AppointmentDTO create(AppointmentDTO request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        if (appointmentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new IllegalArgumentException("Opération déjà traitée");
        }

        ClientDto client = clientsRequest.getClientById(request.clientId());
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable");
        }

        ServiceDto service = serviceRequest.getServiceById(request.serviceId());
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable");
        }
        if (!service.isAvailable()) {
            throw new IllegalArgumentException("Le service demandé est indisponible");
        }

        Appointment appointment = new Appointment(request.clientId(), request.serviceId(),
                request.appointmentDate(), AppointmentStatus.PENDING, request.notes());
        appointment.setIdempotencyKey(idempotencyKey);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        registerConfirmationAfterCommit(savedAppointment, client, service);
        return AppointmentDTO.from(savedAppointment);
    }

    private void registerConfirmationAfterCommit(Appointment appointment, ClientDto client, ServiceDto service) {
        NotificationEmailDto notification = new NotificationEmailDto(
                client.getEmail(),
                "Confirmation de votre rendez-vous",
                "Bonjour " + client.getFirstName() + ",\n\n"
                        + "Votre rendez-vous a été enregistré avec succès.\n\n"
                        + "Numéro du rendez-vous : " + appointment.getId() + "\n"
                        + "Date : " + appointment.getAppointmentDate() + "\n"
                        + "Service : " + service.getName() + "\n"
                        + "Statut : " + appointment.getStatus() + "\n\n"
                        + "Merci pour votre confiance.");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notificationPublisher.envoyer(notification);
                } catch (RuntimeException exception) {
                    log.error("Rendez-vous {} enregistré, mais la publication de la notification a échoué",
                            appointment.getId(), exception);
                }
            }
        });
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("La clé d'idempotence est obligatoire");
        }
        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("La clé d'idempotence ne peut pas dépasser 255 caractères");
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findAll() {
        return appointmentRepository.findAll().stream().map(AppointmentDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDTO findById(Long id) {
        return AppointmentDTO.from(getAppointment(id));
    }

    @Transactional(readOnly = true)
    public AppointmentDTOFull getAppointmentFullById(Long id) {
        Appointment appointment = getAppointment(id);
        AppointmentDTOFull result = appointmentMapperFull.toFullDto(appointment);

        ClientDto client = clientsRequest.getClientById(appointment.getClientId());
        result.setClientName(client.getFirstName() + " " + client.getLastName());
        result.setClientEmail(client.getEmail());

        ServiceDto service = serviceRequest.getServiceById(appointment.getServiceId());
        result.setServiceName(service.getName());
        result.setServiceDescription(service.getDescription());
        result.setServicePrice(service.getPrice());
        result.setServiceDuration(service.getDuration());

        return result;
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
