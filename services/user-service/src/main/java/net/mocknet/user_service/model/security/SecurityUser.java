package net.mocknet.user_service.model.security;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.mocknet.user_service.model.user.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class SecurityUser implements UserDetails {

    private UUID id;
    private String username;
    private String password;
    private String email;
    private boolean enabled;
    private List<SimpleGrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.username = user.getLogin();
        this.password = user.getPasswordHash();
        this.email = user.getEmail();
        this.enabled = user.isVerified() && user.isActive();
        this.authorities = user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority(r.getName().name()))
            .collect(Collectors.toList());
    }
}
