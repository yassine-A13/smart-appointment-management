package com.smartappointment.clientservice.client;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientDTO create(ClientDTO request) {
        Client client = new Client(
                request.firstName(), request.lastName(), request.email(), request.phone());
        return ClientDTO.from(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> findAll() {
        return clientRepository.findAll().stream().map(ClientDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        return ClientDTO.from(getClient(id));
    }

    public ClientDTO update(Long id, ClientDTO request) {
        Client client = getClient(id);
        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        return ClientDTO.from(clientRepository.save(client));
    }

    public void delete(Long id) {
        clientRepository.delete(getClient(id));
    }

    private Client getClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }
}
