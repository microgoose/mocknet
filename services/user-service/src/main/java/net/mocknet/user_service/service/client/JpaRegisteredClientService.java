package net.mocknet.user_service.service.client;

import net.mocknet.user_service.repository.ClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
public class JpaRegisteredClientService implements RegisteredClientRepository {

    private final ClientRepository clientRepository;
    private final ClientConvertor clientConvertor;

    public JpaRegisteredClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
        ClassLoader classLoader = JpaRegisteredClientService.class.getClassLoader();
        this.clientConvertor = new ClientConvertor(classLoader);
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        if (registeredClient == null) {
            throw new IllegalArgumentException("Клиент не может быть null");
        }
        this.clientRepository.save(clientConvertor.toEntity(registeredClient));
    }

    @Override
    public RegisteredClient findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID клиента не может быть пустым");
        }
        return this.clientRepository
            .findById(id)
            .map(clientConvertor::toObject)
            .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client ID не может быть пустым");
        }
        return this.clientRepository
            .findByClientId(clientId)
            .map(clientConvertor::toObject)
            .orElse(null);
    }
}