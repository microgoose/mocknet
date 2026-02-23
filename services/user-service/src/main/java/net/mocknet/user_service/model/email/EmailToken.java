package net.mocknet.user_service.model.email;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.mocknet.user_service.model.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_tokens")
@Getter
@Setter
public class EmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailTokenType type;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private OffsetDateTime usedAt;

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}