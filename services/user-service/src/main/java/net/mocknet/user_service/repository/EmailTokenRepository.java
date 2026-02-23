package net.mocknet.user_service.repository;

import net.mocknet.user_service.model.email.EmailToken;
import net.mocknet.user_service.model.email.EmailTokenType;
import net.mocknet.user_service.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {
    Optional<EmailToken> findByTokenAndType(UUID token, EmailTokenType type);

    boolean existsByTokenAndType(UUID token, EmailTokenType type);

    List<EmailToken> findAllByUserAndType(User user, EmailTokenType type);

    List<EmailToken> findAllByUser(User user);

    Optional<EmailToken> findByToken(UUID token);
}
