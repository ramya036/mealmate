package com.ramya.mealmate.service;

import com.ramya.mealmate.dto.Dtos.*;
import com.ramya.mealmate.exception.ApiException;
import com.ramya.mealmate.model.Role;
import com.ramya.mealmate.model.User;
import com.ramya.mealmate.repository.UserRepository;
import com.ramya.mealmate.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        // Self-registration always creates a CUSTOMER; admins are provisioned separately.
        User user = users.save(new User(req.email().toLowerCase(), encoder.encode(req.password()),
                req.fullName(), Role.CUSTOMER));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                // Same message for unknown email and wrong password, to avoid account enumeration.
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.issue(u), new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name()));
    }
}
