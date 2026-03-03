package unit.service.client;

import common.TestClientFactory;
import net.mocknet.user_service.model.client.Client;
import net.mocknet.user_service.repository.ClientRepository;
import net.mocknet.user_service.service.client.ClientConvertor;
import net.mocknet.user_service.service.client.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Optional;
import java.util.Set;

import static common.TestClientFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientConvertor clientConvertor;

    @InjectMocks
    private ClientService clientService;

    private RegisteredClient testRegisteredClient;
    private Client testClientEntity;
    private String testId;
    private String testClientId;

    @BeforeEach
    void setUp() {
        testId = TestClientFactory.ID;
        testClientId = TestClientFactory.CLIENT_ID;

        testRegisteredClient = createRegisteredClient();
        testClientEntity = createClient();
    }

    @Nested
    class SaveTests {

        @Test
        void save_WithValidClient_ShouldConvertAndSave() {
            // Arrange
            when(clientConvertor.toEntity(testRegisteredClient)).thenReturn(testClientEntity);
            when(clientRepository.save(testClientEntity)).thenReturn(testClientEntity);

            // Act
            clientService.save(testRegisteredClient);

            // Assert
            InOrder inOrder = inOrder(clientConvertor, clientRepository);

            inOrder.verify(clientConvertor, times(1)).toEntity(testRegisteredClient);
            inOrder.verify(clientRepository, times(1)).save(testClientEntity);
        }

        @Test
        void save_WithNullClient_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Клиент не может быть null");

            verify(clientConvertor, never()).toEntity(any());
            verify(clientRepository, never()).save(any());
        }

        @Test
        void save_WhenRepositoryThrowsException_ShouldPropagateException() {
            // Arrange
            when(clientConvertor.toEntity(testRegisteredClient)).thenReturn(testClientEntity);
            when(clientRepository.save(testClientEntity))
                .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThatThrownBy(() -> clientService.save(testRegisteredClient))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

            verify(clientConvertor, times(1)).toEntity(testRegisteredClient);
            verify(clientRepository, times(1)).save(testClientEntity);
        }

        @Test
        void save_ShouldCallRepositoryWithCorrectEntity() {
            // Arrange
            when(clientConvertor.toEntity(testRegisteredClient)).thenReturn(testClientEntity);
            when(clientRepository.save(testClientEntity)).thenReturn(testClientEntity);

            // Act
            clientService.save(testRegisteredClient);

            // Assert
            verify(clientRepository).save(testClientEntity);
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_WhenClientExists_ShouldReturnRegisteredClient() {
            // Arrange
            when(clientRepository.findById(testId)).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findById(testId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ID);
            assertThat(result.getClientId()).isEqualTo(CLIENT_ID);

            verify(clientRepository, times(1)).findById(testId);
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
        }

        @Test
        void findById_WhenClientDoesNotExist_ShouldReturnNull() {
            // Arrange
            when(clientRepository.findById(testId)).thenReturn(Optional.empty());

            // Act
            RegisteredClient result = clientService.findById(testId);

            // Assert
            assertThat(result).isNull();

            verify(clientRepository, times(1)).findById(testId);
            verify(clientConvertor, never()).toRegisteredClient(any());
        }

        @Test
        void findById_WithNullId_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID клиента не может быть пустым");

            verify(clientRepository, never()).findById(any());
            verify(clientConvertor, never()).toRegisteredClient(any());
        }

        @Test
        void findById_WithEmptyId_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.findById(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID клиента не может быть пустым");

            verify(clientRepository, never()).findById(any());
        }

        @Test
        void findById_WithBlankId_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.findById("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID клиента не может быть пустым");

            verify(clientRepository, never()).findById(any());
        }

        @Test
        void findById_WhenRepositoryReturnsEntityWithNullFields_ShouldStillConvert() {
            // Arrange
            Client entityWithNulls = new Client();
            entityWithNulls.setId(testId);

            when(clientRepository.findById(testId)).thenReturn(Optional.of(entityWithNulls));
            when(clientConvertor.toRegisteredClient(entityWithNulls)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findById(testId);

            // Assert
            assertThat(result).isNotNull();
            verify(clientConvertor, times(1)).toRegisteredClient(entityWithNulls);
        }
    }

    @Nested
    class FindByClientIdTests {

        @Test
        void findByClientId_WhenClientExists_ShouldReturnRegisteredClient() {
            // Arrange
            when(clientRepository.findByClientId(testClientId)).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findByClientId(testClientId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ID);
            assertThat(result.getClientId()).isEqualTo(CLIENT_ID);

            verify(clientRepository, times(1)).findByClientId(testClientId);
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
        }

        @Test
        void findByClientId_WhenClientDoesNotExist_ShouldReturnNull() {
            // Arrange
            when(clientRepository.findByClientId(testClientId)).thenReturn(Optional.empty());

            // Act
            RegisteredClient result = clientService.findByClientId(testClientId);

            // Assert
            assertThat(result).isNull();

            verify(clientRepository, times(1)).findByClientId(testClientId);
            verify(clientConvertor, never()).toRegisteredClient(any());
        }

        @Test
        void findByClientId_WithNullClientId_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.findByClientId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client ID не может быть пустым");

            verify(clientRepository, never()).findByClientId(any());
            verify(clientConvertor, never()).toRegisteredClient(any());
        }

        @Test
        void findByClientId_WithEmptyClientId_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.findByClientId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client ID не может быть пустым");

            verify(clientRepository, never()).findByClientId(any());
        }

        @Test
        void findByClientId_WithBlankClientId_ShouldThrowIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> clientService.findByClientId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client ID не может быть пустым");

            verify(clientRepository, never()).findByClientId(any());
        }

        @Test
        void findByClientId_WhenMultipleClientsHaveSameClientId_ShouldReturnFirst() {
            // Arrange
            Client anotherEntity = createClient();
            anotherEntity.setClientId(testClientId);
            anotherEntity.setClientName("client-456");

            when(clientRepository.findByClientId(testClientId)).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findByClientId(testClientId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ID);

            verify(clientRepository, times(1)).findByClientId(testClientId);
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
            verify(clientConvertor, never()).toRegisteredClient(anotherEntity);
        }
    }

    @Nested
    class IntegrationScenarioTests {

        @Test
        void saveThenFindById_ShouldReturnSameClient() {
            // Arrange
            when(clientConvertor.toEntity(testRegisteredClient)).thenReturn(testClientEntity);
            when(clientRepository.save(testClientEntity)).thenReturn(testClientEntity);
            when(clientRepository.findById(testId)).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act - Save
            clientService.save(testRegisteredClient);

            // Act - Find by ID
            RegisteredClient foundClient = clientService.findById(testId);

            // Assert
            assertThat(foundClient).isNotNull();
            assertThat(foundClient.getId()).isEqualTo(ID);
            assertThat(foundClient.getClientId()).isEqualTo(CLIENT_ID);

            verify(clientConvertor, times(1)).toEntity(testRegisteredClient);
            verify(clientRepository, times(1)).save(testClientEntity);
            verify(clientRepository, times(1)).findById(testId);
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
        }

        @Test
        void saveThenFindByClientId_ShouldReturnSameClient() {
            // Arrange
            when(clientConvertor.toEntity(testRegisteredClient)).thenReturn(testClientEntity);
            when(clientRepository.save(testClientEntity)).thenReturn(testClientEntity);
            when(clientRepository.findByClientId(testClientId)).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act - Save
            clientService.save(testRegisteredClient);

            // Act - Find by client ID
            RegisteredClient foundClient = clientService.findByClientId(testClientId);

            // Assert
            assertThat(foundClient).isNotNull();
            assertThat(foundClient.getId()).isEqualTo(ID);
            assertThat(foundClient.getClientId()).isEqualTo(CLIENT_ID);

            verify(clientConvertor, times(1)).toEntity(testRegisteredClient);
            verify(clientRepository, times(1)).save(testClientEntity);
            verify(clientRepository, times(1)).findByClientId(testClientId);
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
        }

        @Test
        void saveClientWithCustomScopes_ShouldPreserveScopes() {
            // Arrange
            Set<String> customScopes = Set.of("admin", "audit");

            RegisteredClient customClient = RegisteredClient.withId(ID)
                .clientId("client-789")
                .clientSecret(CLIENT_SECRET)
                .clientName("client-789")
                .redirectUris(uris -> uris.addAll(REDIRECT_URIS))
                .authorizationGrantTypes(types -> types.addAll(GRANT_TYPES))
                .scopes(scopes -> scopes.addAll(customScopes))
                .clientIdIssuedAt(CLIENT_ID_ISSUED_AT)
                .clientSecretExpiresAt(CLIENT_SECRET_EXPIRES_AT)
                .build();

            Client customEntity = createClient();
            customEntity.setClientId("client-789");
            customEntity.setClientName("custom-client");
            customEntity.setScopes(String.join(",", customScopes));

            when(clientConvertor.toEntity(customClient)).thenReturn(customEntity);
            when(clientRepository.save(customEntity)).thenReturn(customEntity);
            when(clientRepository.findByClientId("custom-client")).thenReturn(Optional.of(customEntity));
            when(clientConvertor.toRegisteredClient(customEntity)).thenReturn(customClient);

            // Act
            clientService.save(customClient);
            RegisteredClient found = clientService.findByClientId("custom-client");

            // Assert
            assertThat(found).isNotNull();
            assertThat(found.getScopes()).isEqualTo(customScopes);
        }
    }
}