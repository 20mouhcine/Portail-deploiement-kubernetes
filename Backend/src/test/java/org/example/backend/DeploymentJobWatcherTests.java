package org.example.backend;

import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobOperationType;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.ServiceImpl.DeploymentJobClaimService;
import org.example.Deployment.ServiceImpl.DeploymentJobWatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentJobWatcherTests {

    @Mock DeploymentJobRepository jobRepository;
    @Mock DeploymentRepository deploymentRepository;
    @Mock DeploymentJobClaimService claimService;

    @Test
    void finishesRollingOutJobWhenDeploymentHasFailed() {
        UUID deploymentId = UUID.randomUUID();
        Deployment deployment = Deployment.create();
        deployment.setStatus(DeploymentStatus.FAILED);
        DeploymentJob job = DeploymentJob.builder()
                .id(UUID.randomUUID())
                .deploymentId(deploymentId)
                .operationType(JobOperationType.CREATE)
                .status(JobStatus.ROLLING_OUT)
                .idempotencyKey(UUID.randomUUID().toString())
                .updatedAt(LocalDateTime.now())
                .build();

        when(jobRepository.findByStatus(JobStatus.ROLLING_OUT)).thenReturn(List.of(job));
        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));

        DeploymentJobWatcher watcher = new DeploymentJobWatcher(jobRepository, deploymentRepository, claimService);
        ReflectionTestUtils.setField(watcher, "maxRetries", 3);
        ReflectionTestUtils.setField(watcher, "staleAfter", Duration.ofMinutes(5));

        watcher.watchRollingOutJobs();

        verify(claimService).recoverStaleApplying(any(LocalDateTime.class), any(Integer.class));
        verify(jobRepository).save(job);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Kubernetes rollout failed");
    }
}
