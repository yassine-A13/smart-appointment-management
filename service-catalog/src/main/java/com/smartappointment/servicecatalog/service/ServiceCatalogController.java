package com.smartappointment.servicecatalog.service;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @PostMapping
    public ResponseEntity<ServiceDTO> create(@Valid @RequestBody ServiceDTO request) {
        ServiceDTO created = serviceCatalogService.create(request);
        return ResponseEntity.created(URI.create("/services/" + created.id())).body(created);
    }

    @GetMapping
    public List<ServiceDTO> findAll() {
        return serviceCatalogService.findAll();
    }

    @GetMapping("/{id}")
    public ServiceDTO findById(@PathVariable("id") Long id) {
        return serviceCatalogService.findById(id);
    }

    @PutMapping("/{id}")
    public ServiceDTO update(@PathVariable("id") Long id, @Valid @RequestBody ServiceDTO request) {
        return serviceCatalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        serviceCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
