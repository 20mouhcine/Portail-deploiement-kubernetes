package org.example.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import org.example.backend.enums.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "Au moins un rôle est obligatoire")
        Set<RoleName> roles
) {
}
