package org.example.Deployment.Controller;

import lombok.RequiredArgsConstructor;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeploymentEventController {

    private final IDeploymentEventService deploymentEventService;

    @GetMapping(value = "/deployments/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeploymentLogs(@PathVariable UUID id) {
        return deploymentEventService.subscribeToDeploymentLogs(id);
    }

    @GetMapping(value = "/projects/{projectId}/deployments/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProjectDeployments(@PathVariable UUID projectId) {
        return deploymentEventService.subscribeToProjectDeployments(projectId);
    }

    @GetMapping(value = "/deployments/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAllDeployments() {
        return deploymentEventService.subscribeToAllDeployments();
    }
}
