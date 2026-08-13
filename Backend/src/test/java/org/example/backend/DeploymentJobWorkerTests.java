package org.example.backend;

import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobOperationType;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.example.Deployment.ServiceImpl.DeploymentJobClaimService;
import org.example.Deployment.ServiceImpl.DeploymentJobWorker;
import org.example.Deployment.ServiceImpl.DeploymentStatusSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentJobWorkerTests {

    @Mock DeploymentJobRepository jobRepository;
    @Mock DeploymentRepository deploymentRepository;
    @Mock IKubernetesDeploymentService kubernetesDeploymentService;
    @Mock DeploymentStatusSynchronizer statusSynchronizer;
    @Mock DeploymentJobClaimService claimService;

    @Test
    void loadsJobRelationsAndKeepsTheValidationFailure() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID deploymentId = UUID.randomUUID();
        DeploymentJob job = DeploymentJob.builder()
                .id(jobId)
                .deploymentId(deploymentId)
                .operationType(JobOperationType.CREATE)
                .status(JobStatus.APPLYING)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        Deployment deployment = Deployment.create();
        deployment.setStatus(DeploymentStatus.PENDING);

        when(claimService.claimNext()).thenReturn(Optional.of(jobId), Optional.empty());
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(deploymentRepository.findForJobById(deploymentId)).thenReturn(Optional.of(deployment));
        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        when(kubernetesDeploymentService.deploy(deployment))
                .thenThrow(new IllegalArgumentException("Registry not allowed: registry.example.com"));

        DeploymentJobWorker worker = new DeploymentJobWorker(
                jobRepository,
                deploymentRepository,
                kubernetesDeploymentService,
                statusSynchronizer,
                claimService);
        ReflectionTestUtils.setField(worker, "maxRetries", 3);

        worker.processQueuedJobs();

        verify(deploymentRepository).findForJobById(deploymentId);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Registry not allowed: registry.example.com");
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.FAILED);
        verify(jobRepository).save(job);
        verify(deploymentRepository, times(2)).save(deployment);
    }

    @Test
    void updateClearsPreviousFailureBeforeStartingRollout() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID deploymentId = UUID.randomUUID();
        DeploymentJob job = DeploymentJob.builder()
                .id(jobId)
                .deploymentId(deploymentId)
                .operationType(JobOperationType.UPDATE)
                .status(JobStatus.APPLYING)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        Deployment deployment = Deployment.create();
        deployment.setStatus(DeploymentStatus.FAILED);

        when(claimService.claimNext()).thenReturn(Optional.of(jobId), Optional.empty());
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(deploymentRepository.findForJobById(deploymentId)).thenReturn(Optional.of(deployment));

        DeploymentJobWorker worker = new DeploymentJobWorker(
                jobRepository,
                deploymentRepository,
                kubernetesDeploymentService,
                statusSynchronizer,
                claimService);
        ReflectionTestUtils.setField(worker, "maxRetries", 3);

        worker.processQueuedJobs();

        verify(kubernetesDeploymentService).updateSpec(deployment);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING);
        assertThat(job.getStatus()).isEqualTo(JobStatus.ROLLING_OUT);
    }

    @Test
    void restartStoppedDeploymentRestoresOneReplica() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID deploymentId = UUID.randomUUID();
        DeploymentJob job = DeploymentJob.builder()
                .id(jobId)
                .deploymentId(deploymentId)
                .operationType(JobOperationType.RESTART)
                .status(JobStatus.APPLYING)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        Deployment deployment = Deployment.create();
        deployment.setStatus(DeploymentStatus.STOPPED);
        deployment.setReplicas(0);

        when(claimService.claimNext()).thenReturn(Optional.of(jobId), Optional.empty());
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(deploymentRepository.findForJobById(deploymentId)).thenReturn(Optional.of(deployment));

        DeploymentJobWorker worker = new DeploymentJobWorker(
                jobRepository,
                deploymentRepository,
                kubernetesDeploymentService,
                statusSynchronizer,
                claimService);
        ReflectionTestUtils.setField(worker, "maxRetries", 3);

        worker.processQueuedJobs();

        verify(kubernetesDeploymentService).scale(deployment, 1);
        verify(kubernetesDeploymentService).restart(deployment);
        assertThat(deployment.getReplicas()).isEqualTo(1);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING);
        assertThat(job.getStatus()).isEqualTo(JobStatus.ROLLING_OUT);
    }
}
