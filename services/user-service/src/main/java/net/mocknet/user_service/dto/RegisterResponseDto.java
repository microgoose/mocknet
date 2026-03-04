package net.mocknet.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponseDto {

    private UUID id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isVerified;
    private OffsetDateTime createdAt;
}
