package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobOperationType;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentJobWorker {

    private final DeploymentJobRepository jobRepository;
    private final DeploymentRepository deploymentRepository;
    private final IKubernetesDeploymentService kubernetesDeploymentService;
    private final DeploymentStatusSynchronizer statusSynchronizer;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processQueuedJobs() {
        List<DeploymentJob> queuedJobs = jobRepository.findByStatus(JobStatus.QUEUED);
        for (DeploymentJob job : queuedJobs) {
            // Simple backoff: retryCount * 10 seconds
            if (job.getRetryCount() > 0 && job.getUpdatedAt() != null) {
                if (job.getUpdatedAt().plusSeconds(job.getRetryCount() * 10L).isAfter(LocalDateTime.now())) {
                    continue; // skip until backoff is over
                }
            }

            job.setStatus(JobStatus.APPLYING);
            jobRepository.saveAndFlush(job);
            
            try {
                Deployment deployment = deploymentRepository.findById(job.getDeploymentId())
                        .orElseThrow(() -> new IllegalStateException("Deployment not found"));

                switch (job.getOperationType()) {
                    case CREATE:
                        String url = kubernetesDeploymentService.deploy(deployment);
                        deployment.setAccessUrl(url);
                        deploymentRepository.save(deployment);
                        job.setStatus(JobStatus.ROLLING_OUT);
                        break;
                    case UPDATE:
                        kubernetesDeploymentService.updateSpec(deployment);
                        job.setStatus(JobStatus.ROLLING_OUT);
                        break;
                    case SCALE:
                        kubernetesDeploymentService.scale(deployment, job.getTargetReplicas() != null ? job.getTargetReplicas() : deployment.getReplicas());
                        deployment.setReplicas(job.getTargetReplicas() != null ? job.getTargetReplicas() : deployment.getReplicas());
                        deployment.setStatus(job.getTargetReplicas() == 0 ? DeploymentStatus.STOPPED : DeploymentStatus.PENDING);
                        deploymentRepository.save(deployment);
                        job.setStatus(JobStatus.READY);
                        break;
                    case RESTART:
                        kubernetesDeploymentService.restart(deployment);
                        job.setStatus(JobStatus.ROLLING_OUT);
                        break;
                    case DELETE:
                        kubernetesDeploymentService.delete(deployment);
                        deploymentRepository.delete(deployment);
                        job.setStatus(JobStatus.READY);
                        break;
                }
                job.setErrorMessage(null);
                
                if (job.getStatus() == JobStatus.ROLLING_OUT) {
                    statusSynchronizer.resumeTracking(deployment);
                }
                
            } catch (Exception e) {
                log.error("Job {} failed: {}", job.getId(), e.getMessage());
                job.setRetryCount(job.getRetryCount() + 1);
                job.setErrorMessage(e.getMessage());
                if (job.getRetryCount() >= 3) {
                    job.setStatus(JobStatus.FAILED);
                    updateDeploymentStatusToFailed(job.getDeploymentId());
                } else {
                    job.setStatus(JobStatus.QUEUED);
                }
            }
            jobRepository.save(job);
        }
    }

    private void updateDeploymentStatusToFailed(java.util.UUID deploymentId) {
        deploymentRepository.findById(deploymentId).ifPresent(d -> {
            d.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(d);
        });
    }
}
