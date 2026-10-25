package com.ramya.mealmate.security;

/** The authenticated principal placed in the SecurityContext by {@link JwtAuthFilter}. */
public record AuthUser(Long id, String email, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
