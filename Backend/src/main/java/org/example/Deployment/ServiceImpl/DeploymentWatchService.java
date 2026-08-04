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
    private final org.example.Deployment.Repository.DeploymentJobRepository deploymentJobRepository;
    private final IDeploymentEventService deploymentEventService;
    private final org.example.Deployment.Service.IKubernetesDeploymentService kubernetesDeploymentService;

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

        kubernetesClient.services()
                .inAnyNamespace()
                .withLabel("deployment-id")
                .watch(new Watcher<io.fabric8.kubernetes.api.model.Service>() {
                    @Override
                    public void eventReceived(Action action, io.fabric8.kubernetes.api.model.Service service) {
                        try {
                            String deploymentIdStr = service.getMetadata().getLabels().get("deployment-id");
                            if (deploymentIdStr == null) return;
                            UUID deploymentId = UUID.fromString(deploymentIdStr);
                            
                            handleServiceEvent(action, service, deploymentId);
                        } catch (Exception e) {
                            log.error("Error processing service event", e);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        if (cause != null) {
                            log.error("Service watcher closed with error", cause);
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

        if (newStatus == DeploymentStatus.RUNNING || newStatus == DeploymentStatus.STOPPED) {
            // Resolve rolling out jobs
            deploymentJobRepository.findByStatus(org.example.Deployment.Enums.JobStatus.ROLLING_OUT).stream()
                .filter(job -> job.getDeploymentId().equals(deploymentId))
                .forEach(job -> {
                    job.setStatus(org.example.Deployment.Enums.JobStatus.READY);
                    deploymentJobRepository.save(job);
                });
        }
    }

    @Transactional
    public void handleServiceEvent(Watcher.Action action, io.fabric8.kubernetes.api.model.Service service, UUID deploymentId) {
        org.example.Deployment.Entity.Deployment dbDeployment = deploymentRepository.findById(deploymentId).orElse(null);
        if (dbDeployment == null) return;

        if (action == Watcher.Action.DELETED) {
            if (dbDeployment.getAccessUrl() != null) {
                dbDeployment.setAccessUrl(null);
                deploymentRepository.save(dbDeployment);
            }
        } else {
            String newUrl = kubernetesDeploymentService.getAccessUrl(dbDeployment);
            if (newUrl != null && !newUrl.equals(dbDeployment.getAccessUrl())) {
                dbDeployment.setAccessUrl(newUrl);
                deploymentRepository.save(dbDeployment);
                log.info("Updated access URL for deployment {} to {}", deploymentId, newUrl);
            } else if (newUrl == null && dbDeployment.getAccessUrl() != null) {
                dbDeployment.setAccessUrl(null);
                deploymentRepository.save(dbDeployment);
                log.info("Cleared access URL for deployment {}", deploymentId);
            }
        }
    }
}
