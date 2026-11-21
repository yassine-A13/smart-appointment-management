package com.smartappointment.appointmentservice.appointment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartappointment.appointmentservice.exception.GlobalExceptionHandler;
import feign.FeignException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class AppointmentControllerTest {

    private AppointmentService appointmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appointmentService = mock(AppointmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(appointmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndLocation() throws Exception {
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        AppointmentDTO request = new AppointmentDTO(null, 12L, 34L, date, AppointmentStatus.PENDING, "checkup");
        AppointmentDTO created = new AppointmentDTO(7L, 12L, 34L, date, AppointmentStatus.PENDING, "checkup");
        when(appointmentService.create(request, "postman-key-1")).thenReturn(created);

        mockMvc.perform(post("/appointments")
                        .header("Idempotency-Key", "postman-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":12,"serviceId":34,"appointmentDate":"%s","status":"PENDING","notes":"checkup"}
                                """.formatted(date)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertEquals(
                        "/appointments/7", result.getResponse().getHeader("Location")));
    }

    @Test
    void simpleAndDetailedConsultationReturn200() throws Exception {
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        AppointmentDTO simple = new AppointmentDTO(7L, 12L, 34L, date, AppointmentStatus.PENDING, "checkup");
        AppointmentDTOFull detailed = new AppointmentDTOFull();
        detailed.setId(7L);
        detailed.setClientId(12L);
        detailed.setServiceId(34L);
        detailed.setClientName("Ada Lovelace");
        detailed.setServiceName("Consultation");
        when(appointmentService.findById(7L)).thenReturn(simple);
        when(appointmentService.getAppointmentFullById(7L)).thenReturn(detailed);

        mockMvc.perform(get("/appointments/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(12));
        mockMvc.perform(get("/appointments/7/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.serviceName").value("Consultation"));
    }

    @Test
    void detailedConsultationForMissingAppointmentReturns404() throws Exception {
        when(appointmentService.getAppointmentFullById(404L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Appointment not found"));

        mockMvc.perform(get("/appointments/404/detail"))
                .andExpect(status().isNotFound());
    }

    @Test
    void feignCommunicationFailureReturns503() throws Exception {
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(-1);
        when(appointmentService.getAppointmentFullById(7L)).thenThrow(failure);

        mockMvc.perform(get("/appointments/7/detail"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Échec de communication avec un service"));
    }

    @Test
    void otherFeignHttpErrorReturns502() throws Exception {
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(500);
        when(appointmentService.getAppointmentFullById(7L)).thenThrow(failure);

        mockMvc.perform(get("/appointments/7/detail"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Erreur HTTP du service distant : 500"));
    }
}
