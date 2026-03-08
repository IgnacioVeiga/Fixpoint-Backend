package com.fixpoint.auth.controller;

import com.fixpoint.auth.dto.AuthTokenResponse;
import com.fixpoint.auth.dto.RegisterRequest;
import com.fixpoint.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthTokenResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        return authService.registerForDev(request, response);
    }
}
