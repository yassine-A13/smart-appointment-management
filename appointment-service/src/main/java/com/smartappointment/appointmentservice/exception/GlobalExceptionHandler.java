package com.smartappointment.appointmentservice.exception;

import feign.FeignException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "Requête invalide" : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String, String>> handleFeignNotFound(FeignException.NotFound exception) {
        return error(HttpStatus.NOT_FOUND, "Ressource externe introuvable");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeignException(FeignException exception) {
        if (exception.status() == -1) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Échec de communication avec un service");
        }
        if (exception.status() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Service ou instance indisponible");
        }
        return error(HttpStatus.BAD_GATEWAY, "Erreur HTTP du service distant : " + exception.status());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
