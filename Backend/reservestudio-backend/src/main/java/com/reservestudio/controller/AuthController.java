package com.reservestudio.controller;

import com.reservestudio.config.AuthInterceptor;
import com.reservestudio.dto.Dtos.LoginRequest;
import com.reservestudio.dto.Dtos.SignupRequest;
import com.reservestudio.dto.Dtos.UserResponse;
import com.reservestudio.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UserResponse user = authService.login(request);

        // Session fixation saldırısına karşı: varsa eski oturumu kullan ama ID'sini yenile
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(AuthInterceptor.SESSION_USER_ID, user.id());
        return user;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public UserResponse me(@RequestAttribute(AuthInterceptor.REQUEST_USER_ID) Long userId) {
        return authService.getById(userId);
    }
}
