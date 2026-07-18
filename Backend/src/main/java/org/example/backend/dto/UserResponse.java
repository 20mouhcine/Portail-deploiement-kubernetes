package org.example.backend.dto;

import org.example.backend.enums.RoleName;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        Set<RoleName> roles
) {
}