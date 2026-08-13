package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Enums.DeploymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.deployment.jobs.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class DeploymentJobWatcher {

    private final DeploymentJobRepository jobRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentJobClaimService claimService;

    @Value("${app.deployment.jobs.max-retries:3}")
    private int maxRetries;

    @Value("${app.deployment.jobs.stale-after:5m}")
    private Duration staleAfter;

    @Scheduled(fixedDelayString = "${app.deployment.jobs.watch-delay:5000}")
    @Transactional
    public void watchRollingOutJobs() {
        claimService.recoverStaleApplying(LocalDateTime.now().minus(staleAfter), maxRetries);
        List<DeploymentJob> rollingOutJobs = jobRepository.findByStatus(JobStatus.ROLLING_OUT);
        
        for (DeploymentJob job : rollingOutJobs) {
            boolean deploymentFailed = deploymentRepository.findById(job.getDeploymentId())
                    .map(deployment -> deployment.getStatus() == DeploymentStatus.FAILED)
                    .orElse(false);
            if (deploymentFailed) {
                log.warn("Deployment {} failed while job {} was rolling out.", job.getDeploymentId(), job.getId());
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Kubernetes rollout failed. Check the deployment logs.");
                jobRepository.save(job);
                continue;
            }

            // If job has been rolling out for more than 5 minutes, mark as FAILED
            if (job.getUpdatedAt() != null && job.getUpdatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                log.warn("Job {} timed out while ROLLING_OUT. Marking as FAILED.", job.getId());
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Operation timed out waiting for Kubernetes rollout.");
                jobRepository.save(job);
                deploymentRepository.findById(job.getDeploymentId()).ifPresent(deployment -> {
                    deployment.setStatus(DeploymentStatus.FAILED);
                    deploymentRepository.save(deployment);
                });
            }
        }
    }
}
