package net.mocknet.user_service.unit.service.security;

import net.mocknet.user_service.model.security.SecurityUser;
import net.mocknet.user_service.model.user.Role;
import net.mocknet.user_service.model.user.RoleName;
import net.mocknet.user_service.model.user.User;
import net.mocknet.user_service.repository.UserRepository;
import net.mocknet.user_service.service.security.SecurityUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

import static net.mocknet.user_service.common.factory.TestUserFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUserService securityUserService;

    private User testUser;
    private String testEmail;
    private Set<Role> userRoles;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";

        Role userRole = Role.builder().name(RoleName.ROLE_USER).build();
        Role adminRole = Role.builder().name(RoleName.ROLE_ADMIN).build();
        userRoles = new HashSet<>(List.of(userRole, adminRole));

        testUser = createUser();
        testUser.setEmail(testEmail);
        testUser.setLogin("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setVerified(true);
        testUser.setActive(true);
        testUser.setRoles(userRoles);
    }

    @Test
    void loadUserByUsername_WhenUserExistsAndIsActiveAndVerified_ShouldReturnSecurityUser() {
        // Arrange
        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(testEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(SecurityUser.class);

        SecurityUser securityUser = (SecurityUser) result;
        assertThat(securityUser.getId()).isEqualTo(testUser.getId());
        assertThat(securityUser.getUsername()).isEqualTo(testUser.getLogin());
        assertThat(securityUser.getPassword()).isEqualTo(testUser.getPasswordHash());
        assertThat(securityUser.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(securityUser.isEnabled()).isTrue();

        // Verify authorities
        assertThat(securityUser.getAuthorities()).hasSize(2);
        assertThat(securityUser.getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactlyInAnyOrder(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name());

        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(testEmail, true, true);
    }

    @Test
    void loadUserByUsername_WhenUserNotFound_ShouldThrowUsernameNotFoundException() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmailAndActiveAndVerified(nonExistentEmail, true, true))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> securityUserService.loadUserByUsername(nonExistentEmail))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage(nonExistentEmail);

        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(nonExistentEmail, true, true);
    }

    @Test
    void loadUserByUsername_WhenUserIsInactive_ShouldThrowUsernameNotFoundException() {
        // Arrange
        testUser.setActive(false);

        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> securityUserService.loadUserByUsername(testEmail))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage(testEmail);

        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(testEmail, true, true);
    }

    @Test
    void loadUserByUsername_WhenUserIsNotVerified_ShouldThrowUsernameNotFoundException() {
        // Arrange
        testUser.setVerified(false);

        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> securityUserService.loadUserByUsername(testEmail))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage(testEmail);

        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(testEmail, true, true);
    }

    @Test
    void loadUserByUsername_WithNullEmail_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> securityUserService.loadUserByUsername(null))
            .isInstanceOf(Exception.class);

        verify(userRepository, never())
            .findByEmailAndActiveAndVerified(anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void loadUserByUsername_WithEmptyEmail_ShouldThrowUsernameNotFoundException() {
        // Arrange
        String emptyEmail = "";
        when(userRepository.findByEmailAndActiveAndVerified(emptyEmail, true, true))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> securityUserService.loadUserByUsername(emptyEmail))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage(emptyEmail);

        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(emptyEmail, true, true);
    }

    @Test
    void loadUserByUsername_WhenUserHasSingleRole_ShouldReturnSecurityUserWithOneAuthority() {
        // Arrange
        Role singleRole = Role.builder()
            .id((short) 1)
            .name(RoleName.ROLE_USER)
            .build();
        testUser.setRoles(Set.of(singleRole));

        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(testEmail);

        // Assert
        assertThat(result).isNotNull();
        SecurityUser securityUser = (SecurityUser) result;

        assertThat(securityUser.getAuthorities()).hasSize(1);
        assertThat(securityUser.getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactly(RoleName.ROLE_USER.name());
    }

    @Test
    void loadUserByUsername_WhenUserHasNoRoles_ShouldReturnSecurityUserWithEmptyAuthorities() {
        // Arrange
        testUser.setRoles(new HashSet<>());

        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(testEmail);

        // Assert
        assertThat(result).isNotNull();
        SecurityUser securityUser = (SecurityUser) result;

        assertThat(securityUser.getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_ShouldMapAllUserFieldsCorrectly() {
        // Arrange
        UUID expectedId = UUID.randomUUID();
        String expectedLogin = "complex_user";
        String expectedEmail = "complex@example.com";
        String expectedPassword = "$2a$10$complex.hash.value";

        testUser.setId(expectedId);
        testUser.setLogin(expectedLogin);
        testUser.setEmail(expectedEmail);
        testUser.setPasswordHash(expectedPassword);
        testUser.setVerified(true);
        testUser.setActive(true);

        when(userRepository.findByEmailAndActiveAndVerified(expectedEmail, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(expectedEmail);

        // Assert
        SecurityUser securityUser = (SecurityUser) result;

        assertThat(securityUser.getId()).isEqualTo(expectedId);
        assertThat(securityUser.getUsername()).isEqualTo(expectedLogin);
        assertThat(securityUser.getEmail()).isEqualTo(expectedEmail);
        assertThat(securityUser.getPassword()).isEqualTo(expectedPassword);
        assertThat(securityUser.isEnabled()).isTrue();
        assertThat(securityUser.isAccountNonExpired()).isTrue();
        assertThat(securityUser.isAccountNonLocked()).isTrue();
        assertThat(securityUser.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_WhenCalledMultipleTimes_ShouldCallRepositoryEachTime() {
        // Arrange
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        User user1 = createUser();
        user1.setEmail(email1);
        user1.setVerified(true);
        user1.setActive(true);

        User user2 = createUser();
        user2.setEmail(email2);
        user2.setVerified(true);
        user2.setActive(true);

        when(userRepository.findByEmailAndActiveAndVerified(email1, true, true))
            .thenReturn(Optional.of(user1));
        when(userRepository.findByEmailAndActiveAndVerified(email2, true, true))
            .thenReturn(Optional.of(user2));

        // Act
        UserDetails result1 = securityUserService.loadUserByUsername(email1);
        UserDetails result2 = securityUserService.loadUserByUsername(email2);

        // Assert
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();

        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(email1, true, true);
        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(email2, true, true);
    }

    @Test
    void loadUserByUsername_WhenDatabaseErrorOccurs_ShouldPropagateException() {
        // Arrange
        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThatThrownBy(() -> securityUserService.loadUserByUsername(testEmail))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database connection error");
    }

    @Test
    void loadUserByUsername_WithEmailContainingWhitespace_ShouldSearchWithWhitespace() {
        // Arrange
        String emailWithSpace = " test@example.com ";
        when(userRepository.findByEmailAndActiveAndVerified(emailWithSpace, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(emailWithSpace);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository, times(1))
            .findByEmailAndActiveAndVerified(emailWithSpace, true, true);
    }

    @Test
    void loadUserByUsername_WhenUserIsBothActiveAndVerified_ShouldSetEnabledTrue() {
        // Arrange
        testUser.setActive(true);
        testUser.setVerified(true);

        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(testEmail);

        // Assert
        SecurityUser securityUser = (SecurityUser) result;
        assertThat(securityUser.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_ShouldAlwaysReturnNonNullUserDetails() {
        // Arrange
        when(userRepository.findByEmailAndActiveAndVerified(testEmail, true, true))
            .thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = securityUserService.loadUserByUsername(testEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isNotNull();
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getAuthorities()).isNotNull();
    }
}