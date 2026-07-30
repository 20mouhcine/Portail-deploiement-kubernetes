package org.example.Deployment.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.DeploymentResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class DeploymentEventPublisher {

    private static final long TIMEOUT = 0L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ok", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void publish(DeploymentResponse deployment) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("deployment-updated")
                        .data(deployment, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
                log.debug("Removed closed deployment SSE emitter", e);
            }
        }
    }
}
