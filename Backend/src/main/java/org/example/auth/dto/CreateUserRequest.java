package org.example.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.auth.enums.RoleName;

import java.util.Set;

public record CreateUserRequest(

        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(
                min = 3,
                max = 50,
                message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères"
        )
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Le nom d'utilisateur contient des caractères non autorisés"
        )
        String username,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "L'adresse email est invalide")
        @Size(max = 150, message = "L'adresse email est trop longue")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(
                min = 12,
                max = 128,
                message = "Le mot de passe doit contenir entre 12 et 128 caractères"
        )
        String password,

        @NotEmpty(message = "Au moins un rôle est obligatoire")
        Set<RoleName> roles

) {
}