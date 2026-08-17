package org.example.Deployment.Controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.ApiResponse;
import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentDetailResponse;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.DeploymentJobResponse;
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

@Slf4j
@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final IDeploymentService deploymentService;
    private final IActionHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<DeploymentResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(deploymentService.findAll(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectAccess.canReadDeployment(#id, authentication)")
    public ResponseEntity<ApiResponse<DeploymentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement récupéré avec succès",
                deploymentService.findById(id)
        ));
    }

        @GetMapping("/{id}/details")
        @PreAuthorize("@projectAccess.canReadDeployment(#id, authentication)")
        public ResponseEntity<ApiResponse<DeploymentDetailResponse>> details(@PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Détail du déploiement récupéré avec succès",
                                deploymentService.getDetail(id)
                ));
        }

    @GetMapping("/operations/{operationId}")
    @PreAuthorize("@projectAccess.canReadOperation(#operationId, authentication)")
    public ResponseEntity<ApiResponse<DeploymentJobResponse>> getOperationStatus(@PathVariable UUID operationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Statut de l'opération récupéré avec succès",
                deploymentService.getJobStatus(operationId)
        ));
    }

    @PostMapping
    @PreAuthorize("@projectAccess.canDeploy(#request.projectId, #request.namespace, authentication)")
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
    @PreAuthorize("@projectAccess.canOperateDeployment(#id, authentication)")
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
    @PreAuthorize("@projectAccess.canOperateDeployment(#id, authentication)")
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
    @PreAuthorize("@projectAccess.canOperateDeployment(#id, authentication)")
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
    @PreAuthorize("@projectAccess.canOperateDeployment(#id, authentication)")
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
    @PreAuthorize("@projectAccess.canOperateDeployment(#id, authentication)")
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
    @PreAuthorize("@projectAccess.canOperateDeployment(#id, authentication)")
    public ResponseEntity<ApiResponse<DeploymentResponse>> delete(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse deployment = deploymentService.delete(id);
        record(ActionType.DELETE, "Suppression du déploiement", deployment, authentication, httpRequest);
        log.info("deleting deployment {}", deployment);
        return ResponseEntity.ok(ApiResponse.success(
                "Déploiement supprimé avec succès",
                deployment
        ));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@projectAccess.canReadDeployment(#id, authentication)")
    public ResponseEntity<ApiResponse<String>> logs(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Logs récupérés avec succès",
                deploymentService.getLogs(id)
        ));
    }

    @GetMapping("/{id}/pods")
    @PreAuthorize("@projectAccess.canReadDeployment(#id, authentication)")
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
