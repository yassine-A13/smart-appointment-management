package com.smartappointment.appointmentservice.feignClient;

import java.math.BigDecimal;

public class ServiceDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private boolean available;

    public ServiceDto() {
    }

    public ServiceDto(Long id, String name, String description, BigDecimal price, Integer duration, boolean available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.duration = duration;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
