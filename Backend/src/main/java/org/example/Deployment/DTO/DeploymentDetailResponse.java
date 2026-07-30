package org.example.Deployment.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Enums.DeploymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentDetailResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private String name;
    private DeploymentStatus status;
    private String namespace;
    private Integer replicas;
    private String image;
    private Integer port;
    private String cpu;
    private String memory;
    private String accessUrl;
    private Map<String, String> configVariables;
    private Map<String, String> secretVariables;
    private String gitRepository;
    private String gitBranch;
    private String gitCommit;
    private String gitTag;
    private String requestedHostname;
    private String requestedPath;
    private Boolean tlsEnabled;
    private String tlsSecretName;
    private Integer desiredReplicas;
    private Integer availableReplicas;
    private Integer readyReplicas;
    private Integer unavailableReplicas;
    private String failureCause;
    private List<PodResponse> pods;
    private List<DeploymentEventResponse> events;
    private List<DeploymentRevisionResponse> rolloutHistory;
    private LocalDateTime createdAt;
    private String deployedBy;

    public static DeploymentDetailResponse from(
            Deployment deployment,
            List<PodResponse> pods,
            List<DeploymentEventResponse> events,
            List<DeploymentRevisionResponse> rolloutHistory,
            Integer availableReplicas,
            Integer readyReplicas,
            Integer unavailableReplicas,
            String failureCause
    ) {
        return DeploymentDetailResponse.builder()
                .id(deployment.getId())
                .projectId(deployment.getProject().getId())
                .projectName(deployment.getProject().getName())
                .name(deployment.getName())
                .status(deployment.getStatus())
                .namespace(deployment.getNamespace())
                .replicas(deployment.getReplicas())
                .image(deployment.getImage())
                .port(deployment.getPort())
                .cpu(deployment.getCpu())
                .memory(deployment.getMemory())
                .accessUrl(deployment.getAccessUrl())
                .configVariables(deployment.getEnvVariables())
                .secretVariables(deployment.getSecretVariables())
                .gitRepository(deployment.getGitRepository())
                .gitBranch(deployment.getGitBranch())
                .gitCommit(deployment.getGitCommit())
                .gitTag(deployment.getGitTag())
                .requestedHostname(deployment.getRequestedHostname())
                .requestedPath(deployment.getRequestedPath())
                .tlsEnabled(Boolean.TRUE.equals(deployment.getTlsEnabled()))
                .tlsSecretName(deployment.getTlsSecretName())
                .desiredReplicas(deployment.getReplicas())
                .availableReplicas(availableReplicas)
                .readyReplicas(readyReplicas)
                .unavailableReplicas(unavailableReplicas)
                .failureCause(failureCause)
                .pods(pods)
                .events(events)
                .rolloutHistory(rolloutHistory)
                .createdAt(deployment.getCreatedAt())
                .deployedBy(deployment.getDeployedBy().getUsername())
                .build();
    }
}