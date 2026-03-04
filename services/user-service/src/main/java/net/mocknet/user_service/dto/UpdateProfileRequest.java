package net.mocknet.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "{validation.login.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{validation.login.pattern}")
    private String login;

    @Email(message = "{validation.email.invalid}")
    @Size(min = 5, max = 255, message = "{validation.email.size}")
    private String email;

    @Size(max = 100, message = "{validation.firstName.size}")
    private String firstName;

    @Size(max = 100, message = "{validation.lastName.size}")
    private String lastName;

    @URL(message = "{validation.avatarUrl.invalid}")
    @Size(min = 10, max = 100, message = "{validation.avatarUrl.size}")
    private String avatarUrl;
}
