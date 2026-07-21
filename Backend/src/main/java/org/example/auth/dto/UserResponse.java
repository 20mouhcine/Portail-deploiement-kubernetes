package org.example.auth.dto;

import org.example.auth.enums.RoleName;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        Set<RoleName> roles
) {
}