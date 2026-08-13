package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.deployment.jobs.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class DeploymentJobWorker {
    private final DeploymentJobRepository jobRepository;
    private final DeploymentRepository deploymentRepository;
    private final IKubernetesDeploymentService kubernetesDeploymentService;
    private final DeploymentStatusSynchronizer statusSynchronizer;
    private final DeploymentJobClaimService claimService;

    @Value("${app.deployment.jobs.max-retries:3}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${app.deployment.jobs.poll-ms:5000}")
    public void processQueuedJobs() {
        for (int processed = 0; processed < 10; processed++) {
            var jobId = claimService.claimNext();
            if (jobId.isEmpty()) return;
            process(jobId.get());
        }
    }

    void process(UUID jobId) {
        DeploymentJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.APPLYING) return;
        try {
            Deployment deployment = deploymentRepository.findForJobById(job.getDeploymentId())
                    .orElseThrow(() -> new IllegalStateException("Déploiement introuvable"));
            switch (job.getOperationType()) {
                case CREATE -> {
                    markPending(deployment);
                    deployment.setAccessUrl(kubernetesDeploymentService.deploy(deployment));
                    deploymentRepository.save(deployment);
                    job.setStatus(JobStatus.ROLLING_OUT);
                }
                case UPDATE -> {
                    markPending(deployment);
                    kubernetesDeploymentService.updateSpec(deployment);
                    job.setStatus(JobStatus.ROLLING_OUT);
                }
                case SCALE -> {
                    int replicas = job.getTargetReplicas() == null ? deployment.getReplicas() : job.getTargetReplicas();
                    kubernetesDeploymentService.scale(deployment, replicas);
                    deployment.setReplicas(replicas);
                    deployment.setStatus(replicas == 0 ? DeploymentStatus.STOPPED : DeploymentStatus.PENDING);
                    deploymentRepository.save(deployment);
                    job.setStatus(JobStatus.READY);
                }
                case RESTART -> {
                    markPending(deployment);
                    if (deployment.getReplicas() == null || deployment.getReplicas() < 1) {
                        kubernetesDeploymentService.scale(deployment, 1);
                        deployment.setReplicas(1);
                        deploymentRepository.save(deployment);
                    }
                    kubernetesDeploymentService.restart(deployment);
                    job.setStatus(JobStatus.ROLLING_OUT);
                }
                case DELETE -> {
                    kubernetesDeploymentService.delete(deployment);
                    deploymentRepository.delete(deployment);
                    job.setStatus(JobStatus.READY);
                }
            }
            job.setErrorMessage(null);
            job.setNextAttemptAt(null);
            if (job.getStatus() == JobStatus.ROLLING_OUT) statusSynchronizer.resumeTracking(deployment);
        } catch (Exception exception) {
            log.error("Deployment job {} failed", job.getId(), exception);
            int retries = job.getRetryCount() + 1;
            job.setRetryCount(retries);
            job.setErrorMessage(safeMessage(exception));
            job.setStartedAt(null);
            if (!retryable(exception) || retries >= maxRetries) {
                job.setStatus(JobStatus.FAILED);
                deploymentRepository.findById(job.getDeploymentId()).ifPresent(deployment -> {
                    deployment.setStatus(DeploymentStatus.FAILED);
                    deploymentRepository.save(deployment);
                });
            } else {
                job.setStatus(JobStatus.QUEUED);
                job.setNextAttemptAt(LocalDateTime.now().plusSeconds(10L * (1L << Math.min(retries - 1, 5))));
            }
        }
        jobRepository.save(job);
    }

    private void markPending(Deployment deployment) {
        deployment.setStatus(DeploymentStatus.PENDING);
        deploymentRepository.save(deployment);
    }

    private boolean retryable(Exception exception) {
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) return false;
        if (exception instanceof KubernetesClientException kubernetes) {
            int code = kubernetes.getCode();
            return code <= 0 || code == 408 || code == 429 || code >= 500;
        }
        return true;
    }

    private String safeMessage(Exception exception) {
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return exception.getMessage();
        }
        return "L'opération Kubernetes n'a pas pu être terminée";
    }
}
