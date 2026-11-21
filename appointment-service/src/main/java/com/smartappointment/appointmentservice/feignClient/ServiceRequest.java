package com.smartappointment.appointmentservice.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-catalog")
public interface ServiceRequest {

    @GetMapping("/services/{id}")
    ServiceDto getServiceById(@PathVariable("id") Long id);
}
