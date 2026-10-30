package com.reservestudio.service;

import com.reservestudio.dto.Dtos.LoginRequest;
import com.reservestudio.dto.Dtos.SignupRequest;
import com.reservestudio.dto.Dtos.UserResponse;
import com.reservestudio.exception.ApiException;
import com.reservestudio.model.User;
import com.reservestudio.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse signup(SignupRequest request) {
        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Bu e-posta ile zaten bir hesap var");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Aynı anda iki signup gelirse UNIQUE(email) devreye girer
            throw ApiException.conflict("Bu e-posta ile zaten bir hesap var");
        }
        return toResponse(user);
    }

    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> ApiException.unauthorized("E-posta veya şifre hatalı"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("E-posta veya şifre hatalı");
        }
        return toResponse(user);
    }

    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(AuthService::toResponse)
                .orElseThrow(() -> ApiException.unauthorized("Oturum geçersiz"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
