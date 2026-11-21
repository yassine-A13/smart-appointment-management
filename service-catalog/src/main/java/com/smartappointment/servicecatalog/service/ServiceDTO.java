package com.smartappointment.servicecatalog.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ServiceDTO(
        Long id,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotNull @Min(1) Integer duration,
        boolean available) {

    static ServiceDTO from(ServiceOffered service) {
        return new ServiceDTO(service.getId(), service.getName(), service.getDescription(),
                service.getPrice(), service.getDuration(), service.isAvailable());
    }
}
