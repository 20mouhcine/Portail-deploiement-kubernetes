package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeploymentJobClaimService {
    private final DeploymentJobRepository jobRepository;
    private final DeploymentRepository deploymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> claimNext() {
        return jobRepository.findClaimable(LocalDateTime.now(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(job -> {
                    job.setStatus(JobStatus.APPLYING);
                    job.setStartedAt(LocalDateTime.now());
                    job.setErrorMessage(null);
                    jobRepository.saveAndFlush(job);
                    return job.getId();
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStaleApplying(LocalDateTime threshold, int maxRetries) {
        for (DeploymentJob job : jobRepository.findByStatusAndStartedAtBefore(JobStatus.APPLYING, threshold)) {
            int retries = job.getRetryCount() + 1;
            job.setRetryCount(retries);
            job.setStartedAt(null);
            if (retries >= maxRetries) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("L'opération a été interrompue plusieurs fois");
                deploymentRepository.findById(job.getDeploymentId()).ifPresent(deployment -> {
                    deployment.setStatus(DeploymentStatus.FAILED);
                    deploymentRepository.save(deployment);
                });
            } else {
                job.setStatus(JobStatus.QUEUED);
                job.setNextAttemptAt(LocalDateTime.now().plusSeconds(10L * (1L << Math.min(retries - 1, 5))));
            }
        }
    }
}
