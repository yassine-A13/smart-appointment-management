package com.smartappointment.servicecatalog.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public ServiceDTO create(ServiceDTO request) {
        ServiceOffered service = new ServiceOffered(request.name(), request.description(), request.price(),
                request.duration(), request.available());
        return ServiceDTO.from(serviceRepository.save(service));
    }

    @Transactional(readOnly = true)
    public List<ServiceDTO> findAll() {
        return serviceRepository.findAll().stream().map(ServiceDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceDTO findById(Long id) {
        return ServiceDTO.from(getService(id));
    }

    public ServiceDTO update(Long id, ServiceDTO request) {
        ServiceOffered service = getService(id);
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDuration(request.duration());
        service.setAvailable(request.available());
        return ServiceDTO.from(serviceRepository.save(service));
    }

    public void delete(Long id) {
        serviceRepository.delete(getService(id));
    }

    private ServiceOffered getService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }
}
