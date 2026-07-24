package org.example.Deployment.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeploymentRequest {

    @NotNull(message = "Le projet est obligatoire")
    private UUID projectId;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @NotBlank(message = "Le namespace est obligatoire")
    @Size(max = 63, message = "Le namespace ne doit pas dépasser 63 caractères")
    private String namespace;

    @NotNull(message = "Le nombre de réplicas est obligatoire")
    @Min(value = 1, message = "Au moins un réplica est obligatoire")
    @Max(value = 100, message = "Le nombre de réplicas ne doit pas dépasser 100")
    private Integer replicas;

    @NotBlank(message = "L'image est obligatoire")
    @Size(max = 255, message = "L'image ne doit pas dépasser 255 caractères")
    private String image;

    @NotNull(message = "Le port est obligatoire")
    @Min(value = 1, message = "Le port doit être supérieur à 0")
    @Max(value = 65535, message = "Le port doit être inférieur ou égal à 65535")
    private Integer port;

    @NotBlank(message = "La ressource CPU est obligatoire")
    @Size(max = 20, message = "La ressource CPU ne doit pas dépasser 20 caractères")
    private String cpu;

    @NotBlank(message = "La mémoire est obligatoire")
    @Size(max = 20, message = "La mémoire ne doit pas dépasser 20 caractères")
    private String memory;
}
