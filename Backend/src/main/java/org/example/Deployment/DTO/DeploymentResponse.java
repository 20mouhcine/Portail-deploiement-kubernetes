package org.example.Deployment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Enums.DeploymentStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentResponse {

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
    private Map<String, String> envVariables;
    private LocalDateTime createdAt;
    private String deployedBy;
    private UUID operationId;

    public static DeploymentResponse from(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getProject().getId(),
                deployment.getProject().getName(),
                deployment.getName(),
                deployment.getStatus(),
                deployment.getNamespace(),
                deployment.getReplicas(),
                deployment.getImage(),
                deployment.getPort(),
                deployment.getCpu(),
                deployment.getMemory(),
                deployment.getAccessUrl(),
                deployment.getEnvVariables(),
                deployment.getCreatedAt(),
                deployment.getDeployedBy().getUsername(),
                null
        );
    }

    public static DeploymentResponse from(Deployment deployment, UUID operationId) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getProject().getId(),
                deployment.getProject().getName(),
                deployment.getName(),
                deployment.getStatus(),
                deployment.getNamespace(),
                deployment.getReplicas(),
                deployment.getImage(),
                deployment.getPort(),
                deployment.getCpu(),
                deployment.getMemory(),
                deployment.getAccessUrl(),
                deployment.getEnvVariables(),
                deployment.getCreatedAt(),
                deployment.getDeployedBy().getUsername(),
                operationId
        );
    }
}
