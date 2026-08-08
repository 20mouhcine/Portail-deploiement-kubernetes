package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentEvent;
import org.example.Deployment.Enums.DeploymentEventLevel;
import org.example.Deployment.Enums.DeploymentEventSource;
import org.example.Deployment.Repository.DeploymentEventRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentEventServiceImpl implements IDeploymentEventService {

    private final DeploymentEventRepository deploymentEventRepository;
    private final DeploymentRepository deploymentRepository;

    private final Map<UUID, List<SseEmitter>> deploymentLogEmitters = new ConcurrentHashMap<>();
    private final Map<UUID, List<SseEmitter>> projectEmitters = new ConcurrentHashMap<>();

    private static final long TIMEOUT = 60 * 60 * 1000L; // 1 hour

    @Override
    public SseEmitter subscribeToDeploymentLogs(UUID deploymentId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        List<SseEmitter> emitters = deploymentLogEmitters.computeIfAbsent(deploymentId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        // Send historical logs on connection
        List<DeploymentEvent> history = deploymentEventRepository.findByDeploymentIdOrderByTimestampAsc(deploymentId);
        try {
            for (DeploymentEvent event : history) {
                emitter.send(SseEmitter.event().name("log").data(event));
            }
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }
    @Override
@Transactional(readOnly = true)
public List<DeploymentEvent> findByDeploymentIdOrderByTimestampDesc(UUID deploymentId) {
    return deploymentEventRepository.findByDeploymentIdOrderByTimestampDesc(deploymentId);
}

    @Override
    public SseEmitter subscribeToProjectDeployments(UUID projectId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        List<SseEmitter> emitters = projectEmitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }
    
    private final List<SseEmitter> globalEmitters = new CopyOnWriteArrayList<>();

    @Scheduled(fixedDelayString = "${app.sse.heartbeat-ms:25000}")
    public void sendHeartbeat() {
        deploymentLogEmitters.values().forEach(this::sendHeartbeat);
        projectEmitters.values().forEach(this::sendHeartbeat);
        sendHeartbeat(globalEmitters);
    }
    
    public SseEmitter subscribeToAllDeployments() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        globalEmitters.add(emitter);

        emitter.onCompletion(() -> globalEmitters.remove(emitter));
        emitter.onTimeout(() -> globalEmitters.remove(emitter));
        emitter.onError((e) -> globalEmitters.remove(emitter));

        return emitter;
    }

    @Override
    @Transactional
    public void saveAndPublishLog(UUID deploymentId, DeploymentEventLevel level, DeploymentEventSource source, String message) {
        DeploymentEvent event = DeploymentEvent.builder()
                .deploymentId(deploymentId)
                .level(level)
                .source(source)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        
        DeploymentEvent savedEvent = deploymentEventRepository.save(event);

        List<SseEmitter> emitters = deploymentLogEmitters.get(deploymentId);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("log").data(savedEvent));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }
    }

    @Override
    public void publishDeploymentStatusChange(UUID projectId, UUID deploymentId, String status) {
        Map<String, String> data = Map.of(
                "deploymentId", deploymentId.toString(),
                "status", status
        );

        // Project stream
        List<SseEmitter> pEmitters = projectEmitters.get(projectId);
        if (pEmitters != null) {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : pEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("status_change").data(data));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            pEmitters.removeAll(deadEmitters);
        }

        // Global stream
        List<SseEmitter> deadGlobalEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : globalEmitters) {
            try {
                emitter.send(SseEmitter.event().name("status_change").data(data));
            } catch (IOException e) {
                deadGlobalEmitters.add(emitter);
            }
        }
        globalEmitters.removeAll(deadGlobalEmitters);
    }

    @Override
    public void publishDeploymentUpdated(DeploymentResponse deployment) {
        publishDeploymentStatusChange(
                deployment.getProjectId(),
                deployment.getId(),
                deployment.getStatus().name()
        );

        List<SseEmitter> pEmitters = projectEmitters.get(deployment.getProjectId());
        if (pEmitters != null) {
            sendDeploymentUpdated(pEmitters, deployment);
        }

        sendDeploymentUpdated(globalEmitters, deployment);
    }

    private void sendDeploymentUpdated(List<SseEmitter> emitters, DeploymentResponse deployment) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("deployment_updated").data(deployment));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    private void sendHeartbeat(List<SseEmitter> emitters) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException exception) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
