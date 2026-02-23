package net.mocknet.user_service.repository;

import net.mocknet.user_service.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndActiveAndVerified(String email, boolean active, boolean verified);
}
