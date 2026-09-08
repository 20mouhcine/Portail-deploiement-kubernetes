package org.example.Deployment.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.ApiResponse;
import org.example.Deployment.DTO.ClusterMetricsResponse;
import org.example.Deployment.Service.IKubernetesMetricsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final IKubernetesMetricsService metricsService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "metrics-sse-poller");
        t.setDaemon(true);
        return t;
    });

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1 hour
    private static final long POLL_INTERVAL_SECONDS = 10;

    /**
     * One-shot endpoint for initial metric snapshot.
     */
    @GetMapping("/cluster")
    public ResponseEntity<ApiResponse<ClusterMetricsResponse>> getClusterMetrics() {
        return ResponseEntity.ok(ApiResponse.success(
                "Métriques du cluster récupérées avec succès",
                metricsService.getClusterMetrics()
        ));
    }

    /**
     * SSE stream that pushes cluster metrics every 10 seconds.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send initial data immediately
        try {
            ClusterMetricsResponse initial = metricsService.getClusterMetrics();
            emitter.send(SseEmitter.event().name("metrics").data(initial));
        } catch (IOException e) {
            emitters.remove(emitter);
        } catch (Exception e) {
            log.error("Error fetching initial metrics for SSE client", e);
        }

        // Start polling if this is the first subscriber
        startPollingIfNeeded();

        return emitter;
    }

    private volatile boolean pollingStarted = false;

    private synchronized void startPollingIfNeeded() {
        if (pollingStarted) return;
        pollingStarted = true;

        scheduler.scheduleWithFixedDelay(() -> {
            if (emitters.isEmpty()) return;

            try {
                ClusterMetricsResponse metrics = metricsService.getClusterMetrics();
                List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event().name("metrics").data(metrics));
                    } catch (IOException e) {
                        deadEmitters.add(emitter);
                    }
                }

                emitters.removeAll(deadEmitters);
            } catch (Exception e) {
                log.error("Error polling cluster metrics", e);
            }
        }, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
}
