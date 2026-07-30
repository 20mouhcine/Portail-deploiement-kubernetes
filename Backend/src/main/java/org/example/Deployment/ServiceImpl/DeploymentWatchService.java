package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentWatchService {

    private final KubernetesClient kubernetesClient;
    private final DeploymentRepository deploymentRepository;
    private final IDeploymentEventService deploymentEventService;

    @PostConstruct
    public void startWatch() {
        kubernetesClient.apps().deployments()
                .inAnyNamespace()
                .withLabel("deployment-id")
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Deployment deployment) {
                        try {
                            String deploymentIdStr = deployment.getMetadata().getLabels().get("deployment-id");
                            if (deploymentIdStr == null) return;
                            UUID deploymentId = UUID.fromString(deploymentIdStr);
                            
                            handleDeploymentEvent(action, deployment, deploymentId);
                        } catch (Exception e) {
                            log.error("Error processing deployment event", e);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        if (cause != null) {
                            log.error("Deployment watcher closed with error", cause);
                        }
                    }
                });
    }

    @Transactional
    public void handleDeploymentEvent(Watcher.Action action, Deployment deployment, UUID deploymentId) {
        org.example.Deployment.Entity.Deployment dbDeployment = deploymentRepository.findById(deploymentId).orElse(null);
        if (dbDeployment == null) return;

        int desired = deployment.getSpec() != null && deployment.getSpec().getReplicas() != null ? deployment.getSpec().getReplicas() : 1;
        int available = deployment.getStatus() != null && deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;

        DeploymentStatus previousStatus = dbDeployment.getStatus();
        DeploymentStatus newStatus;

        if (action == Watcher.Action.DELETED) {
            newStatus = DeploymentStatus.STOPPED;
        } else if (desired == 0) {
            newStatus = DeploymentStatus.STOPPED;
        } else if (available >= desired) {
            newStatus = DeploymentStatus.RUNNING;
        } else {
            newStatus = DeploymentStatus.PENDING;
        }

        if (previousStatus != newStatus) {
            dbDeployment.setStatus(newStatus);
            deploymentRepository.save(dbDeployment);
            
            // Publish status change for project stream
            if (dbDeployment.getProject() != null) {
                deploymentEventService.publishDeploymentStatusChange(dbDeployment.getProject().getId(), deploymentId, newStatus.name());
            }
        }
    }
}
