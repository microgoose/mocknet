package net.mocknet.user_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class ProfileDto {
    private UUID id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private OffsetDateTime lastLoginAt;
}
