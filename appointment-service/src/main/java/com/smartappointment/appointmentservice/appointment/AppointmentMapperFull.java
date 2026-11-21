package com.smartappointment.appointmentservice.appointment;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapperFull {

    @Mapping(target = "clientName", ignore = true)
    @Mapping(target = "clientEmail", ignore = true)
    @Mapping(target = "serviceName", ignore = true)
    @Mapping(target = "serviceDescription", ignore = true)
    @Mapping(target = "servicePrice", ignore = true)
    @Mapping(target = "serviceDuration", ignore = true)
    AppointmentDTOFull toFullDto(Appointment appointment);
}
