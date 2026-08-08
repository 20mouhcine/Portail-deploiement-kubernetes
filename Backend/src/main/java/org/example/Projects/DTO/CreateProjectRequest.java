package org.example.Projects.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

import java.util.Set;
import java.util.UUID;

/**
 * @author pc
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @NotBlank(message = "Le dépôt est obligatoire")
    @Size(max = 500, message = "L'URL du dépôt est trop longue")
    private String repository;

    private UUID owner_id;

    private Set<@Pattern(regexp = "[a-z0-9]([-a-z0-9]*[a-z0-9])?", message = "Namespace Kubernetes invalide") String> allowedNamespaces;

    private Set<@Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "Nom d'utilisateur invalide") String> allowedUsers;

    private String environmentType;

    private String deploymentPolicy;

    private String cpuQuota;

    private String memoryQuota;

    @Min(value = 1, message = "Le quota de pods doit être supérieur à zéro")
    private Integer podQuota;

}
