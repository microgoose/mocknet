package net.mocknet.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {

    @NotBlank(message = "{validation.login.notBlank}")
    @Size(min = 3, max = 50, message = "{validation.login.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{validation.login.pattern}")
    private String login;

    @NotBlank(message = "{validation.email.notBlank}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.password.notBlank}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    private String password;

    @Size(max = 100, message = "{validation.firstName.size}")
    private String firstName;

    @Size(max = 100, message = "{validation.lastName.size}")
    private String lastName;
}
