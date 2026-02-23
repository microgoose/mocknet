package net.mocknet.user_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class RegisterResponseDto {

    private UUID id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private boolean isVerified;
    private OffsetDateTime createdAt;
}
