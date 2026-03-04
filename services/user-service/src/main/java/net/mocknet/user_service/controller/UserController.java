package net.mocknet.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.mocknet.user_service.dto.ProfileDto;
import net.mocknet.user_service.dto.UpdateProfileRequest;
import net.mocknet.user_service.model.security.SecurityUser;
import net.mocknet.user_service.service.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("{id}")
    public ProfileDto getProfile(@PathVariable UUID id) {
        return userService.getProfile(id);
    }

    @PatchMapping("{id}")
    public ProfileDto updateProfile(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateProfileRequest dto,
                                    @AuthenticationPrincipal SecurityUser securityUser) {

        return userService.updateProfile(id, dto, securityUser.getId());
    }
}
