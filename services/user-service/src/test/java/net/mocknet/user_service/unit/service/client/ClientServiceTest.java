package net.mocknet.user_service.unit.service.client;

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

import static net.mocknet.user_service.common.factory.TestClientFactory.createClient;
import static net.mocknet.user_service.common.factory.TestClientFactory.createRegisteredClient;
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

    @BeforeEach
    void setUp() {
        testClientEntity = createClient();

        testRegisteredClient = createRegisteredClient(
            testClientEntity.getId(),
            testClientEntity.getClientId(),
            testClientEntity.getClientName()
        );
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
            when(clientRepository.findById(testClientEntity.getId())).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findById(testClientEntity.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testClientEntity.getId());
            assertThat(result.getClientId()).isEqualTo(testClientEntity.getClientId());

            verify(clientRepository, times(1)).findById(testClientEntity.getId());
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
        }

        @Test
        void findById_WhenClientDoesNotExist_ShouldReturnNull() {
            // Arrange
            when(clientRepository.findById(testClientEntity.getId())).thenReturn(Optional.empty());

            // Act
            RegisteredClient result = clientService.findById(testClientEntity.getId());

            // Assert
            assertThat(result).isNull();

            verify(clientRepository, times(1)).findById(testClientEntity.getId());
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
            entityWithNulls.setId(testClientEntity.getId());

            when(clientRepository.findById(testClientEntity.getId())).thenReturn(Optional.of(entityWithNulls));
            when(clientConvertor.toRegisteredClient(entityWithNulls)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findById(testClientEntity.getId());

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
            when(clientRepository.findByClientId(testClientEntity.getClientId())).thenReturn(Optional.of(testClientEntity));
            when(clientConvertor.toRegisteredClient(testClientEntity)).thenReturn(testRegisteredClient);

            // Act
            RegisteredClient result = clientService.findByClientId(testClientEntity.getClientId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testClientEntity.getId());
            assertThat(result.getClientId()).isEqualTo(testClientEntity.getClientId());

            verify(clientRepository, times(1)).findByClientId(testClientEntity.getClientId());
            verify(clientConvertor, times(1)).toRegisteredClient(testClientEntity);
        }

        @Test
        void findByClientId_WhenClientDoesNotExist_ShouldReturnNull() {
            // Arrange
            when(clientRepository.findByClientId(testClientEntity.getClientId())).thenReturn(Optional.empty());

            // Act
            RegisteredClient result = clientService.findByClientId(testClientEntity.getClientId());

            // Assert
            assertThat(result).isNull();

            verify(clientRepository, times(1)).findByClientId(testClientEntity.getClientId());
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
    }
}