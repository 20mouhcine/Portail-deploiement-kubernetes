package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Enums.DeploymentEventLevel;
import org.example.Deployment.Enums.DeploymentEventSource;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.kubernetes.watchers.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PodWatchService {

    private final KubernetesClient kubernetesClient;
    private final IDeploymentEventService deploymentEventService;
    private final DeploymentLogService deploymentLogService;
    private final KubernetesPodFailureDetector failureDetector;
    private final DeploymentStatusSynchronizer deploymentStatusSynchronizer;

    @PostConstruct
    public void startWatch() {
        kubernetesClient.pods()
                .inAnyNamespace()
                .withLabel("deployment-id")
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Pod pod) {
                        try {
                            String deploymentIdStr = pod.getMetadata().getLabels().get("deployment-id");
                            if (deploymentIdStr == null) return;
                            UUID deploymentId = UUID.fromString(deploymentIdStr);
                            
                            handlePodEvent(action, pod, deploymentId);
                        } catch (Exception e) {
                            log.error("Error processing pod event", e);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        if (cause != null) {
                            log.error("Pod watcher closed with error", cause);
                        }
                    }
                });
    }

    private void handlePodEvent(Watcher.Action action, Pod pod, UUID deploymentId) {
        if (pod.getStatus() == null) return;
        String phase = pod.getStatus().getPhase();
        
        if (action == Watcher.Action.ADDED) {
            deploymentEventService.saveAndPublishLog(deploymentId, DeploymentEventLevel.INFO, DeploymentEventSource.KUBERNETES, "Pod scheduled: " + pod.getMetadata().getName());
        }

        if (action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED) {
            List<ContainerStatus> statuses = pod.getStatus().getContainerStatuses();
            var failure = failureDetector.detect(pod);
            if (failure.isPresent()) {
                deploymentEventService.saveAndPublishLog(
                        deploymentId,
                        DeploymentEventLevel.ERROR,
                        DeploymentEventSource.KUBERNETES,
                        "Erreur Kubernetes : " + failure.get().description());
                deploymentStatusSynchronizer.markFailed(deploymentId);
            } else if (statuses != null) {
                for (ContainerStatus status : statuses) {
                    if (status.getState() != null) {
                        if (status.getState().getWaiting() != null) {
                            String reason = status.getState().getWaiting().getReason();
                            if ("ContainerCreating".equals(reason)) {
                                deploymentEventService.saveAndPublishLog(deploymentId, DeploymentEventLevel.INFO, DeploymentEventSource.KUBERNETES, "Creating container...");
                            }
                        } else if (status.getState().getRunning() != null && Boolean.TRUE.equals(status.getReady())) {
                            // We can use a cache to avoid duplicate running messages, or just rely on phase=Running below
                        }
                    }
                }
            }

            if (failure.isEmpty() && "Running".equals(phase)) {
                boolean allReady = true;
                if (statuses != null) {
                    for (ContainerStatus status : statuses) {
                        if (!Boolean.TRUE.equals(status.getReady())) {
                            allReady = false;
                            break;
                        }
                    }
                }
                
                if (allReady) {
                    deploymentEventService.saveAndPublishLog(deploymentId, DeploymentEventLevel.SUCCESS, DeploymentEventSource.KUBERNETES, "Pod is running and ready.");
                    deploymentLogService.startLogWatch(pod.getMetadata().getNamespace(), pod.getMetadata().getName(), deploymentId);
                }
            } else if (failure.isEmpty() && "Failed".equals(phase)) {
                deploymentEventService.saveAndPublishLog(deploymentId, DeploymentEventLevel.ERROR, DeploymentEventSource.KUBERNETES, "Pod failed.");
                deploymentStatusSynchronizer.markFailed(deploymentId);
                deploymentLogService.stopLogWatch(pod.getMetadata().getName());
            }
        } else if (action == Watcher.Action.DELETED) {
            deploymentEventService.saveAndPublishLog(deploymentId, DeploymentEventLevel.INFO, DeploymentEventSource.KUBERNETES, "Pod terminated.");
            deploymentLogService.stopLogWatch(pod.getMetadata().getName());
        }
    }
}
