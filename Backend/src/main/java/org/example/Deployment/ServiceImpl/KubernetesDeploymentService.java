package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.springframework.stereotype.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Logger;
/**
 * @author pc
 **/
@Service
@AllArgsConstructor
public class KubernetesDeploymentService implements IKubernetesDeploymentService {
    private final KubernetesClient kubernetesClient;
 @Override
public String deploy(Deployment deployment) throws InterruptedException {
    io.fabric8.kubernetes.api.model.apps.Deployment k8sDeployment = new DeploymentBuilder()
            .withNewMetadata()
            .withName(deployment.getName())
            .withNamespace(deployment.getNamespace())
            .addToLabels("deployment-id", deployment.getId().toString())
            .endMetadata()
            .withNewSpec()
            .withReplicas(deployment.getReplicas())
            .withNewSelector()
            .addToMatchLabels("app", deployment.getName())
            .endSelector()
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", deployment.getName())
            .addToLabels("deployment-id", deployment.getId().toString())
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(deployment.getName())
            .withImage(deployment.getImage())
            .addNewPort().withContainerPort(deployment.getPort()).endPort()
            .withNewResources()
            .addToRequests("cpu", new Quantity(deployment.getCpu()))
            .addToRequests("memory", new Quantity(deployment.getMemory()))
            .addToLimits("cpu", new Quantity(deployment.getCpu()))
            .addToLimits("memory", new Quantity(deployment.getMemory()))
            .endResources()
            .addAllToEnv(toEnvVarList(deployment))
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build();

    kubernetesClient.apps().deployments()
            .inNamespace(deployment.getNamespace())
            .resource(k8sDeployment)
            .createOr(existing -> existing.update());

    io.fabric8.kubernetes.api.model.Service service = new ServiceBuilder()
            .withNewMetadata()
            .withName(deployment.getName())
            .withNamespace(deployment.getNamespace())
            .addToLabels("deployment-id", deployment.getId().toString())
            .endMetadata()
            .withNewSpec()
            .withType("NodePort")
            .addToSelector("app", deployment.getName())
            .addNewPort()
            .withPort(deployment.getPort())
            .withTargetPort(new IntOrString(deployment.getPort()))
            .endPort()
            .endSpec()
            .build();

    kubernetesClient.services()
            .inNamespace(deployment.getNamespace())
            .resource(service)
            .createOr(existing -> existing.update());

    io.fabric8.kubernetes.api.model.Service created = kubernetesClient.services()
            .inNamespace(deployment.getNamespace())
            .withName(deployment.getName())
            .get();

    Logger logger = Logger.getLogger(KubernetesDeploymentService.class.getName());
    logger.info("Service created: " + created);

    return buildAccessUrl(created);
}

private String buildAccessUrl(io.fabric8.kubernetes.api.model.Service service) {
    if (service == null || service.getSpec() == null || service.getSpec().getPorts().isEmpty()) {
        return null;
    }

    Integer nodePort = service.getSpec().getPorts().get(0).getNodePort();
    if (nodePort == null) return null;

    return "http://" + getNodeIp() + ":" + nodePort;
}
    
    private List<EnvVar> toEnvVarList(Deployment deployment) {
                Map<String, String> envVariables = deployment.getEnvVariables();
                Map<String, String> secretVariables = deployment.getSecretVariables();

                if ((envVariables == null || envVariables.isEmpty()) && (secretVariables == null || secretVariables.isEmpty())) {
                        return List.of();
                }

                return java.util.stream.Stream.concat(
                                                envVariables == null ? java.util.stream.Stream.empty() : envVariables.entrySet().stream(),
                                                secretVariables == null ? java.util.stream.Stream.empty() : secretVariables.entrySet().stream())
                .map(e -> new EnvVar(e.getKey(), e.getValue(), null))
                .collect(Collectors.toList());
    }

    @Override
    public String getAccessUrl(Deployment deployment) {
        io.fabric8.kubernetes.api.model.Service service = kubernetesClient.services()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .get();

        if (service == null) return null;

        return buildAccessUrl(service);
    }

 
    private String getNodeIp() {
        return kubernetesClient.nodes().list().getItems().stream()
                .flatMap(n -> n.getStatus().getAddresses().stream())
                .filter(addr -> "InternalIP".equals(addr.getType()))
                .map(NodeAddress::getAddress)
                .findFirst()
                .orElse(URI.create(kubernetesClient.getMasterUrl().toString()).getHost());
    }
    public String getStatus(Deployment deployment) {
        io.fabric8.kubernetes.api.model.apps.Deployment d = kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .get();

        if (d == null) return "UNKNOWN";

        int desired = d.getSpec().getReplicas();
        int available = d.getStatus().getAvailableReplicas() != null
                ? d.getStatus().getAvailableReplicas() : 0;

        return available == desired ? "RUNNING" : "PENDING";
    }

    // 3. PODS — list pods belonging to this deployment
    public List<Pod> getPods(Deployment deployment) {
        return kubernetesClient.pods()
                .inNamespace(deployment.getNamespace())
                .withLabel("app", deployment.getName())
                .list()
                .getItems();
    }

    @Override
    public void updateSpec(Deployment deployment) {
        io.fabric8.kubernetes.api.model.apps.Deployment current = kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .get();
        if (current == null) {
            throw new KubernetesClientException("Kubernetes deployment not found: " + deployment.getName());
        }

        io.fabric8.kubernetes.api.model.apps.Deployment updated = new DeploymentBuilder(current)
                .editSpec()
                .withReplicas(deployment.getReplicas())
                .editTemplate()
                .editSpec()
                .editFirstContainer()
                .withImage(deployment.getImage())
                .withPorts(new ContainerPortBuilder().withContainerPort(deployment.getPort()).build())
                .withEnv(toEnvVarList(deployment))
                .withNewResources()
                .addToRequests("cpu", new Quantity(deployment.getCpu()))
                .addToRequests("memory", new Quantity(deployment.getMemory()))
                .addToLimits("cpu", new Quantity(deployment.getCpu()))
                .addToLimits("memory", new Quantity(deployment.getMemory()))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .resource(updated)
                .update();

        io.fabric8.kubernetes.api.model.Service service = kubernetesClient.services()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .get();
        if (service != null) {
            kubernetesClient.services()
                    .inNamespace(deployment.getNamespace())
                    .withName(deployment.getName())
                    .edit(existing -> new ServiceBuilder(existing)
                            .editSpec()
                            .editFirstPort()
                            .withPort(deployment.getPort())
                            .withTargetPort(new IntOrString(deployment.getPort()))
                            .endPort()
                            .endSpec()
                            .build());
        }
    }
    // 4. SCALE
    public void scale(Deployment deployment, int replicas) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .scale(replicas);
    }

    // 5. STOP — scale to 0 (no separate k8s concept for "stopped")
    public void stop(Deployment deployment) {
        scale(deployment, 0);
    }

    // 6. RESTART — rolling restart
    public void restart(Deployment deployment) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .rolling()
                .restart();
    }

    // 7. UPDATE IMAGE
    public void updateImage(Deployment deployment, String newImage) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .edit(d -> new DeploymentBuilder(d)
                        .editSpec().editTemplate().editSpec()
                        .editFirstContainer().withImage(newImage).endContainer()
                        .endSpec().endTemplate().endSpec()
                        .build());
    }

    // 8. DELETE — remove Deployment + Service
    public void delete(Deployment deployment) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .delete();

        kubernetesClient.services()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .delete();
    }

    // 9. LOGS — fetch logs from the first pod of this deployment
    public String getLogs(Deployment deployment) {
        List<Pod> pods = getPods(deployment);
        if (pods.isEmpty()) return "";

        String podName = pods.get(0).getMetadata().getName();
        return kubernetesClient.pods()
                .inNamespace(deployment.getNamespace())
                .withName(podName)
                .getLog();
    }


}
