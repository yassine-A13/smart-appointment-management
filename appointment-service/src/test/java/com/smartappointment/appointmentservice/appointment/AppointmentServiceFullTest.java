package com.smartappointment.appointmentservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartappointment.appointmentservice.exception.GlobalExceptionHandler;
import com.smartappointment.appointmentservice.feignClient.ClientDto;
import com.smartappointment.appointmentservice.feignClient.ClientsRequest;
import com.smartappointment.appointmentservice.feignClient.ServiceDto;
import com.smartappointment.appointmentservice.feignClient.ServiceRequest;
import com.smartappointment.appointmentservice.messaging.NotificationEmailDto;
import com.smartappointment.appointmentservice.messaging.NotificationPublisher;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.amqp.AmqpException;

class AppointmentServiceFullTest {

    private AppointmentRepository appointmentRepository;
    private ClientsRequest clientsRequest;
    private ServiceRequest serviceRequest;
    private AppointmentMapperFull appointmentMapperFull;
    private NotificationPublisher notificationPublisher;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        clientsRequest = mock(ClientsRequest.class);
        serviceRequest = mock(ServiceRequest.class);
        appointmentMapperFull = new AppointmentMapperFullImpl();
        notificationPublisher = mock(NotificationPublisher.class);
        appointmentService = new AppointmentService(
                appointmentRepository, clientsRequest, serviceRequest, appointmentMapperFull, notificationPublisher);
    }

    @Test
    void returnsAppointmentEnrichedWithClientAndServiceData() {
        LocalDateTime date = LocalDateTime.now().plusDays(2);
        Appointment appointment = appointment(12L, 34L, date);
        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        when(serviceRequest.getServiceById(34L)).thenReturn(
                new ServiceDto(34L, "Consultation", "Initial visit", new BigDecimal("45.00"), 30, true));

        AppointmentDTOFull result = appointmentService.getAppointmentFullById(7L);

        assertEquals(12L, result.getClientId());
        assertEquals(34L, result.getServiceId());
        assertEquals(date, result.getAppointmentDate());
        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        assertEquals("notes", result.getNotes());
        assertEquals("Ada Lovelace", result.getClientName());
        assertEquals("ada@example.com", result.getClientEmail());
        assertEquals("Consultation", result.getServiceName());
        assertEquals("Initial visit", result.getServiceDescription());
        assertEquals(new BigDecimal("45.00"), result.getServicePrice());
        assertEquals(30, result.getServiceDuration());

        // Remote enrichment is only applied to the response DTO.
        assertEquals("notes", appointment.getNotes());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    void validCreationChecksIdempotencyThenRemoteServicesAndStartsPending() {
        String key = "tp4-create-001";
        LocalDateTime date = LocalDateTime.now().plusDays(3);
        AppointmentDTO request = new AppointmentDTO(null, 12L, 34L, date, AppointmentStatus.COMPLETED, "notes");
        when(appointmentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        when(serviceRequest.getServiceById(34L)).thenReturn(
                new ServiceDto(34L, "Consultation", "Initial visit", new BigDecimal("45.00"), 30, true));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 77L);
            return saved;
        });

        TransactionSynchronizationManager.initSynchronization();
        AppointmentDTO created;
        try {
            created = appointmentService.create(request, key);
            assertEquals(77L, created.id());
            verify(notificationPublisher, never()).envoyer(any(NotificationEmailDto.class));

            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        assertEquals(12L, created.clientId());
        assertEquals(34L, created.serviceId());
        assertEquals(date, created.appointmentDate());
        assertEquals(AppointmentStatus.PENDING, created.status());
        var order = inOrder(appointmentRepository, clientsRequest, serviceRequest);
        order.verify(appointmentRepository).findByIdempotencyKey(key);
        order.verify(clientsRequest).getClientById(12L);
        order.verify(serviceRequest).getServiceById(34L);
        order.verify(appointmentRepository).save(any(Appointment.class));
        var notificationCaptor = org.mockito.ArgumentCaptor.forClass(NotificationEmailDto.class);
        verify(notificationPublisher).envoyer(notificationCaptor.capture());
        assertEquals("ada@example.com", notificationCaptor.getValue().destinataire());
        assertEquals("Confirmation de votre rendez-vous", notificationCaptor.getValue().objet());
        org.junit.jupiter.api.Assertions.assertTrue(notificationCaptor.getValue().contenu().contains("Bonjour Ada,"));
        org.junit.jupiter.api.Assertions.assertTrue(notificationCaptor.getValue().contenu().contains("Numéro du rendez-vous : 77"));
        org.junit.jupiter.api.Assertions.assertTrue(notificationCaptor.getValue().contenu().contains("Service : Consultation"));
        org.junit.jupiter.api.Assertions.assertTrue(notificationCaptor.getValue().contenu().contains("Statut : PENDING"));
    }

    @Test
    void invalidIdempotencyKeysReturnBadRequestBeforeRemoteCalls() {
        AppointmentDTO request = validRequest();
        List<String> invalidKeys = java.util.Arrays.asList(null, "", "   ", "k".repeat(256));

        for (String key : invalidKeys) {
            assertThrows(IllegalArgumentException.class, () -> appointmentService.create(request, key));
        }

        verify(appointmentRepository, never()).findByIdempotencyKey(any());
        verify(clientsRequest, never()).getClientById(anyLong());
        verify(serviceRequest, never()).getServiceById(anyLong());
    }

    @Test
    void duplicateIdempotencyKeyReturnsBadRequestBeforeRemoteCalls() {
        String key = "already-used";
        when(appointmentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(appointment(12L, 34L, LocalDateTime.now().plusDays(1))));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> appointmentService.create(validRequest(), key));

        assertEquals("Opération déjà traitée", error.getMessage());
        verify(notificationPublisher, never()).envoyer(any(NotificationEmailDto.class));
        verify(clientsRequest, never()).getClientById(anyLong());
        verify(serviceRequest, never()).getServiceById(anyLong());
    }

    @Test
    void publisherFailureAfterCommitDoesNotTurnCreatedAppointmentIntoFailure() {
        when(appointmentRepository.findByIdempotencyKey("rabbit-down")).thenReturn(Optional.empty());
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        when(serviceRequest.getServiceById(34L)).thenReturn(
                new ServiceDto(34L, "Consultation", "Initial visit", new BigDecimal("45.00"), 30, true));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 78L);
            return saved;
        });
        doThrow(new AmqpException("RabbitMQ indisponible"))
                .when(notificationPublisher).envoyer(any(NotificationEmailDto.class));

        TransactionSynchronizationManager.initSynchronization();
        try {
            AppointmentDTO created = appointmentService.create(validRequest(), "rabbit-down");
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> synchronizations.forEach(TransactionSynchronization::afterCommit));
            assertEquals(78L, created.id());
            verify(notificationPublisher).envoyer(any(NotificationEmailDto.class));
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Test
    void rollbackCompletionDoesNotPublishNotification() {
        when(appointmentRepository.findByIdempotencyKey("rolled-back")).thenReturn(Optional.empty());
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        when(serviceRequest.getServiceById(34L)).thenReturn(
                new ServiceDto(34L, "Consultation", "Initial visit", new BigDecimal("45.00"), 30, true));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            appointmentService.create(validRequest(), "rolled-back");
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        verify(notificationPublisher, never()).envoyer(any(NotificationEmailDto.class));
    }

    @Test
    void missingClientIsMappedToNotFound() {
        when(appointmentRepository.findByIdempotencyKey("client-missing")).thenReturn(Optional.empty());
        FeignException.NotFound failure = mock(FeignException.NotFound.class);
        when(clientsRequest.getClientById(12L)).thenThrow(failure);

        FeignException.NotFound thrown = assertThrows(FeignException.NotFound.class,
                () -> appointmentService.create(validRequest(), "client-missing"));
        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleFeignNotFound(thrown);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ressource externe introuvable", response.getBody().get("message"));
    }

    @Test
    void missingCatalogServiceIsMappedToNotFound() {
        when(appointmentRepository.findByIdempotencyKey("service-missing")).thenReturn(Optional.empty());
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        FeignException.NotFound failure = mock(FeignException.NotFound.class);
        when(serviceRequest.getServiceById(34L)).thenThrow(failure);

        FeignException.NotFound thrown = assertThrows(FeignException.NotFound.class,
                () -> appointmentService.create(validRequest(), "service-missing"));
        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleFeignNotFound(thrown);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ressource externe introuvable", response.getBody().get("message"));
    }

    @Test
    void unavailableOfferedServiceIsBadRequest() {
        when(appointmentRepository.findByIdempotencyKey("unavailable")).thenReturn(Optional.empty());
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        when(serviceRequest.getServiceById(34L)).thenReturn(
                new ServiceDto(34L, "Consultation", "Unavailable", new BigDecimal("45.00"), 30, false));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> appointmentService.create(validRequest(), "unavailable"));
        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleIllegalArgument(error);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void otherRemoteHttpErrorsAreMappedToBadGateway() {
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(500);

        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleFeignException(failure);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Erreur HTTP du service distant : 500", response.getBody().get("message"));
    }

    @Test
    void remoteServiceUnavailableStatusMapsToServiceUnavailable() {
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(503);

        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleFeignException(failure);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Service ou instance indisponible", response.getBody().get("message"));
    }

    @Test
    void missingAppointmentReturnsNotFoundWithoutCallingFeign() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> appointmentService.getAppointmentFullById(99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(clientsRequest, never()).getClientById(org.mockito.ArgumentMatchers.anyLong());
        verify(serviceRequest, never()).getServiceById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void unavailableClientServiceIsMappedToServiceUnavailable() {
        Appointment appointment = appointment(12L, 34L, LocalDateTime.now().plusDays(1));
        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(-1);
        when(clientsRequest.getClientById(12L)).thenThrow(failure);

        FeignException thrown = assertThrows(
                FeignException.class, () -> appointmentService.getAppointmentFullById(7L));
        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleFeignException(thrown);

        assertSame(failure, thrown);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Échec de communication avec un service", response.getBody().get("message"));
    }

    @Test
    void unavailableCatalogServiceIsMappedToServiceUnavailable() {
        Appointment appointment = appointment(12L, 34L, LocalDateTime.now().plusDays(1));
        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        when(clientsRequest.getClientById(12L)).thenReturn(
                new ClientDto(12L, "Ada", "Lovelace", "ada@example.com", null, LocalDateTime.now()));
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(-1);
        when(serviceRequest.getServiceById(34L)).thenThrow(failure);

        FeignException thrown = assertThrows(
                FeignException.class, () -> appointmentService.getAppointmentFullById(7L));
        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleFeignException(thrown);

        assertSame(failure, thrown);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Échec de communication avec un service", response.getBody().get("message"));
    }

    private Appointment appointment(Long clientId, Long serviceId, LocalDateTime date) {
        return new Appointment(clientId, serviceId, date, AppointmentStatus.CONFIRMED, "notes");
    }

    private AppointmentDTO validRequest() {
        return new AppointmentDTO(null, 12L, 34L, LocalDateTime.now().plusDays(1), AppointmentStatus.PENDING, "test");
    }
}
