package org.example.Deployment.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScaleDeploymentRequest(
        @NotNull(message = "Le nombre de réplicas est obligatoire")
        @Min(value = 1, message = "Au moins un réplica est obligatoire")
        @Max(value = 100, message = "Le nombre de réplicas ne doit pas dépasser 100")
        Integer replicas
) {
}
