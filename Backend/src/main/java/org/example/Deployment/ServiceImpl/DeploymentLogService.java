package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.Enums.DeploymentEventLevel;
import org.example.Deployment.Enums.DeploymentEventSource;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogService {

    private final KubernetesClient kubernetesClient;
    private final IDeploymentEventService deploymentEventService;
    
    // Keep track of which pods we are already watching to avoid duplicates
    private final Map<String, LogWatch> activeLogWatches = new ConcurrentHashMap<>();

    public void startLogWatch(String namespace, String podName, UUID deploymentId) {
        if (activeLogWatches.containsKey(podName)) {
            return;
        }

        try {
            LogWatch watch = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .watchLog();

            activeLogWatches.put(podName, watch);

            Thread logThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(watch.getOutput()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        DeploymentEventLevel level = DeploymentEventLevel.INFO;
                        String lowerLine = line.toLowerCase();
                        if (lowerLine.contains("error") || lowerLine.contains("exception")) {
                            level = DeploymentEventLevel.ERROR;
                        } else if (lowerLine.contains("warn")) {
                            level = DeploymentEventLevel.WARNING;
                        }
                        
                        deploymentEventService.saveAndPublishLog(deploymentId, level, DeploymentEventSource.CONTAINER, line);
                    }
                } catch (Exception e) {
                    log.error("Error reading logs for pod {}", podName, e);
                } finally {
                    activeLogWatches.remove(podName);
                    watch.close();
                }
            });
            logThread.setDaemon(true);
            logThread.start();
        } catch (Exception e) {
            log.error("Failed to start log watch for pod {}", podName, e);
        }
    }
    
    public void stopLogWatch(String podName) {
        LogWatch watch = activeLogWatches.remove(podName);
        if (watch != null) {
            watch.close();
        }
    }
}
