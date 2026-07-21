package org.example.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import org.example.auth.enums.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "Au moins un rôle est obligatoire")
        Set<RoleName> roles
) {
}
