package net.mocknet.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.dto.RegisterRequestDto;
import net.mocknet.user_service.dto.RegisterResponseDto;
import net.mocknet.user_service.service.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void verify(@RequestParam UUID token) {
        authService.acceptVerification(token);
    }
}
