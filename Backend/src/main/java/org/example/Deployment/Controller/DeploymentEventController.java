package org.example.Deployment.Controller;

import lombok.RequiredArgsConstructor;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeploymentEventController {

    private final IDeploymentEventService deploymentEventService;

    @GetMapping(value = "/deployments/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectAccess.canReadDeployment(#id, authentication)")
    public SseEmitter streamDeploymentLogs(@PathVariable UUID id, Authentication authentication) {
        return deploymentEventService.subscribeToDeploymentLogs(id);
    }

    @GetMapping(value = "/projects/{projectId}/deployments/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectAccess.canRead(#projectId, authentication)")
    public SseEmitter streamProjectDeployments(@PathVariable UUID projectId, Authentication authentication) {
        return deploymentEventService.subscribeToProjectDeployments(projectId);
    }

    @GetMapping(value = "/deployments/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter streamAllDeployments() {
        return deploymentEventService.subscribeToAllDeployments();
    }
}
