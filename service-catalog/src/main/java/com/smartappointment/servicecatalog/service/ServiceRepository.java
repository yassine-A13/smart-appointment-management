package com.smartappointment.servicecatalog.service;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceOffered, Long> {
}
