package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentJobWatcher {

    private final DeploymentJobRepository jobRepository;

    @Scheduled(fixedDelay = 60000) // Check every minute
    @Transactional
    public void watchRollingOutJobs() {
        List<DeploymentJob> rollingOutJobs = jobRepository.findByStatus(JobStatus.ROLLING_OUT);
        
        for (DeploymentJob job : rollingOutJobs) {
            // If job has been rolling out for more than 5 minutes, mark as FAILED
            if (job.getUpdatedAt() != null && job.getUpdatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                log.warn("Job {} timed out while ROLLING_OUT. Marking as FAILED.", job.getId());
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Operation timed out waiting for Kubernetes rollout.");
                jobRepository.save(job);
            }
        }
    }
}
