package org.example.Deployment.Controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.Deployment.DTO.ApiResponse;
import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentDetailResponse;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.PodResponse;
import org.example.Deployment.DTO.ScaleDeploymentRequest;
import org.example.Deployment.DTO.UpdateDeploymentRequest;
import org.example.Deployment.Service.IDeploymentService;
import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;
import org.example.History.Service.IActionHistoryService;
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
    private final IActionHistoryService historyService;

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

        @GetMapping("/{id}/details")
        public ResponseEntity<ApiResponse<DeploymentDetailResponse>> details(@PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Détail du déploiement récupéré avec succès",
                                deploymentService.getDetail(id)
                ));
        }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEVOPS','ADMIN')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> create(
            @Valid @RequestBody CreateDeploymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = deploymentService.create(request, authentication.getName());
        record(
                ActionType.CREATE,
                "Création et déploiement sur le cluster Kubernetes",
                response,
                authentication,
                httpRequest
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Déploiement créé et lancé avec succès",
                response
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeploymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = deploymentService.update(id, request);
        record(ActionType.UPDATE, "Modification et mise à jour sur le cluster", response, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement modifié avec succès",
                response
        ));
    }

    @PatchMapping("/{id}/restart")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> restart(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = deploymentService.restart(id);
        record(ActionType.RESTART, "Redémarrage du déploiement demandé", response, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Redémarrage placé en attente",
                response
        ));
    }

    @PatchMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> stop(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = deploymentService.stop(id);
        record(ActionType.UPDATE, "Arrêt du déploiement", response, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement arrêté avec succès",
                response
        ));
    }

    @PatchMapping("/{id}/scale")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> scale(
            @PathVariable UUID id,
            @Valid @RequestBody ScaleDeploymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = deploymentService.scale(id, request.replicas());
        record(
                ActionType.SCALE,
                "Nombre de réplicas modifié à " + request.replicas(),
                response,
                authentication,
                httpRequest
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Mise à l'échelle placée en attente",
                response
        ));
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<DeploymentResponse>> rollback(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = deploymentService.rollback(id);
        record(ActionType.UPDATE, "Rollback vers la revision precedente", response, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Rollback effectué avec succès",
                response
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVOPS')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse deployment = deploymentService.findById(id);
        deploymentService.delete(id);
        record(ActionType.DELETE, "Suppression du déploiement", deployment, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement supprimé avec succès",
                null
        ));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<String>> logs(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Logs récupérés avec succès",
                deploymentService.getLogs(id)
        ));
    }

    @GetMapping("/{id}/pods")
    public ResponseEntity<ApiResponse<List<PodResponse>>> pods(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pods récupérés avec succès",
                deploymentService.getPods(id)
        ));
    }

    private void record(
            ActionType action,
            String details,
            DeploymentResponse deployment,
            Authentication authentication,
            HttpServletRequest request
    ) {
        historyService.record(
                action,
                details,
                TargetType.DEPLOYMENT,
                deployment.getName(),
                authentication.getName(),
                request.getRemoteAddr()
        );
    }
}