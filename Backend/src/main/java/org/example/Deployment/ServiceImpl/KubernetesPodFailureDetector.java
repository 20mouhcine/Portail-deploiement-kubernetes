package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesPodFailureDetector {

    private static final Set<String> FAILURE_REASONS = Set.of(
            "ImagePullBackOff",
            "ErrImagePull",
            "CrashLoopBackOff",
            "CreateContainerConfigError",
            "CreateContainerError",
            "InvalidImageName"
    );

    public Optional<PodFailure> detect(Pod pod) {
        if (pod == null || pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return Optional.empty();
        }

        return pod.getStatus().getContainerStatuses().stream()
                .map(this::waitingFailure)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<PodFailure> waitingFailure(ContainerStatus containerStatus) {
        if (containerStatus.getState() == null || containerStatus.getState().getWaiting() == null) {
            return Optional.empty();
        }

        String reason = containerStatus.getState().getWaiting().getReason();
        if (!FAILURE_REASONS.contains(reason)) {
            return Optional.empty();
        }

        return Optional.of(new PodFailure(reason, containerStatus.getState().getWaiting().getMessage()));
    }

    public record PodFailure(String reason, String message) {
        public String description() {
            return message == null || message.isBlank() ? reason : reason + " - " + message;
        }
    }
}
