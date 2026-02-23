package net.mocknet.user_service.service.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.model.security.SecurityUser;
import net.mocknet.user_service.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityUserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return new SecurityUser(userRepository
            .findByEmailAndActiveAndVerified(email, true, true)
            .orElseThrow(() -> new UsernameNotFoundException(email))
        );
    }
}
