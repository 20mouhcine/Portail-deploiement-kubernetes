package org.example.Deployment.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.JobOperationType;
import org.example.Deployment.Enums.JobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentJobResponse {
    private UUID id;
    private UUID deploymentId;
    private JobOperationType operationType;
    private JobStatus status;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeploymentJobResponse from(DeploymentJob job) {
        return DeploymentJobResponse.builder()
                .id(job.getId())
                .deploymentId(job.getDeploymentId())
                .operationType(job.getOperationType())
                .status(job.getStatus())
                .retryCount(job.getRetryCount())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
