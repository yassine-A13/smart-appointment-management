package com.smartappointment.clientservice.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record ClientDTO(
        Long id,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phone,
        LocalDateTime createdAt) {

    static ClientDTO from(Client client) {
        return new ClientDTO(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getCreatedAt());
    }
}
