package org.example.backend;

import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.example.Deployment.ServiceImpl.KubernetesPodFailureDetector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KubernetesPodFailureDetectorTests {

    private final KubernetesPodFailureDetector detector = new KubernetesPodFailureDetector();

    @Test
    void detectsImagePullBackOffAndKeepsTheKubernetesMessage() {
        Pod pod = waitingPod("ImagePullBackOff", "pull access denied");

        var failure = detector.detect(pod);

        assertThat(failure).isPresent();
        assertThat(failure.orElseThrow().reason()).isEqualTo("ImagePullBackOff");
        assertThat(failure.orElseThrow().description()).contains("pull access denied");
    }

    @Test
    void detectsCrashLoopBackOff() {
        assertThat(detector.detect(waitingPod("CrashLoopBackOff", null))).isPresent();
    }

    @Test
    void ignoresAContainerThatIsStillBeingCreated() {
        assertThat(detector.detect(waitingPod("ContainerCreating", null))).isEmpty();
    }

    private Pod waitingPod(String reason, String message) {
        return new PodBuilder()
                .withNewStatus()
                .withContainerStatuses(new ContainerStatusBuilder()
                        .withName("application")
                        .withState(new ContainerStateBuilder()
                                .withNewWaiting()
                                .withReason(reason)
                                .withMessage(message)
                                .endWaiting()
                                .build())
                        .build())
                .endStatus()
                .build();
    }
}
