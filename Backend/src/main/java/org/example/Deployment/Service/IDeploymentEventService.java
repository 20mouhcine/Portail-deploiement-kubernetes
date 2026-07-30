package org.example.Deployment.Service;

import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.Entity.DeploymentEvent;
import org.example.Deployment.Enums.DeploymentEventLevel;
import org.example.Deployment.Enums.DeploymentEventSource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.List;

public interface IDeploymentEventService {
    SseEmitter subscribeToDeploymentLogs(UUID deploymentId);
    SseEmitter subscribeToProjectDeployments(UUID projectId);
    SseEmitter subscribeToAllDeployments();
    
    void saveAndPublishLog(UUID deploymentId, DeploymentEventLevel level, DeploymentEventSource source, String message);
    void publishDeploymentStatusChange(UUID projectId, UUID deploymentId, String status);
    void publishDeploymentUpdated(DeploymentResponse deployment);
        List<DeploymentEvent> findByDeploymentIdOrderByTimestampDesc(UUID deploymentId);

}
