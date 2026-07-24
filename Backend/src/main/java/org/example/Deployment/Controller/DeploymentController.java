package org.example.Deployment.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.Deployment.DTO.ApiResponse;
import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.ScaleDeploymentRequest;
import org.example.Deployment.DTO.UpdateDeploymentRequest;
import org.example.Deployment.Service.IDeploymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final IDeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<DeploymentResponse>> findAll() {
        return ResponseEntity.ok(deploymentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeploymentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement récupéré avec succès",
                deploymentService.findById(id)
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> create(
            @Valid @RequestBody CreateDeploymentRequest request,
            Authentication authentication
    ) {
        DeploymentResponse response = deploymentService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Configuration de déploiement créée avec succès",
                response
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeploymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Configuration de déploiement modifiée avec succès",
                deploymentService.update(id, request)
        ));
    }

    @PatchMapping("/{id}/restart")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> restart(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Redémarrage placé en attente",
                deploymentService.restart(id)
        ));
    }

    @PatchMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> stop(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement arrêté avec succès",
                deploymentService.stop(id)
        ));
    }

    @PatchMapping("/{id}/scale")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> scale(
            @PathVariable UUID id,
            @Valid @RequestBody ScaleDeploymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Mise à l'échelle placée en attente",
                deploymentService.scale(id, request.replicas())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        deploymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement supprimé avec succès",
                null
        ));
    }
}
