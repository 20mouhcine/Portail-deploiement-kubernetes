package org.example.Deployment.DTO;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PodResponse {

    private String name;
    private String status;
    private String phase;
    private String node;
    private Integer restartCount;
    private Boolean ready;
    private String reason;
    private OffsetDateTime createdAt;

    public static PodResponse from(Pod pod) {
        String status = pod.getStatus() != null && pod.getStatus().getPhase() != null
                ? pod.getStatus().getPhase()
                : "UNKNOWN";

        boolean ready = false;
        String reason = null;
        if (pod.getStatus() != null) {
            ready = pod.getStatus().getContainerStatuses() != null
                && pod.getStatus().getContainerStatuses().stream().allMatch(statusItem -> Boolean.TRUE.equals(statusItem.getReady()));
            reason = pod.getStatus().getReason();
            if (reason == null && pod.getStatus().getContainerStatuses() != null && !pod.getStatus().getContainerStatuses().isEmpty()) {
            reason = pod.getStatus().getContainerStatuses().get(0).getState() != null
                && pod.getStatus().getContainerStatuses().get(0).getState().getWaiting() != null
                ? pod.getStatus().getContainerStatuses().get(0).getState().getWaiting().getReason()
                : null;
            }
        }

        int restartCount = 0;
        if (pod.getStatus() != null) {
            List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            if (containerStatuses != null && !containerStatuses.isEmpty()) {
                restartCount = containerStatuses.get(0).getRestartCount();
            }
        }

        String node = pod.getSpec() != null ? pod.getSpec().getNodeName() : null;

        OffsetDateTime createdAt = pod.getMetadata() != null && pod.getMetadata().getCreationTimestamp() != null
                ? OffsetDateTime.parse(pod.getMetadata().getCreationTimestamp())
                : null;

        return new PodResponse(
                pod.getMetadata().getName(),
                status,
            status,
                node,
                restartCount,
            ready,
            reason,
                createdAt
        );
    }
}