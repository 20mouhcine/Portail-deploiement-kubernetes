package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentStatusSynchronizer {

    private static final int MAX_CONSECUTIVE_FAILURES = 4;

    private final ConcurrentMap<UUID, Integer> consecutiveFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> stoppedDeployments = new ConcurrentHashMap<>();
    private final DeploymentRepository deploymentRepository;
    private final IDeploymentEventService deploymentEventService;

    @Transactional
    public void markFailed(UUID deploymentId) {
        if (deploymentId == null) {
            return;
        }

        deploymentRepository.findById(deploymentId).ifPresent(deployment -> {
            recordStatus(deploymentId, DeploymentStatus.FAILED);
            if (deployment.getStatus() == DeploymentStatus.FAILED) {
                return;
            }

            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
            if (deployment.getProject() != null) {
                deploymentEventService.publishDeploymentStatusChange(
                        deployment.getProject().getId(), deploymentId, DeploymentStatus.FAILED.name());
            }
        });
    }

    public void resumeTracking(Deployment deployment) {
        if (deployment == null || deployment.getId() == null) {
            return;
        }

        consecutiveFailures.remove(deployment.getId());
        stoppedDeployments.remove(deployment.getId());
    }

    public boolean isTrackingStopped(UUID deploymentId) {
        return stoppedDeployments.containsKey(deploymentId);
    }

    public void recordStatus(UUID deploymentId, DeploymentStatus status) {
        if (deploymentId == null) {
            return;
        }

        if (status != DeploymentStatus.FAILED) {
            consecutiveFailures.remove(deploymentId);
            return;
        }

        int failures = consecutiveFailures.merge(deploymentId, 1, Integer::sum);
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            stoppedDeployments.put(deploymentId, true);
            log.warn("Stopped status tracking for deployment {} after {} consecutive failed checks", deploymentId,
                    failures);
        }
    }
}
