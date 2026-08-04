package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.AllArgsConstructor;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
@AllArgsConstructor
public class KubernetesDeploymentService implements IKubernetesDeploymentService {
    private final KubernetesClient kubernetesClient;
    private static final List<String> ALLOWED_REGISTRIES = List.of("docker.io", "gcr.io", "ghcr.io", "quay.io", "registry.k8s.io");

    @Override
    public String deploy(Deployment deployment) throws InterruptedException {
        validateDeploymentConfig(deployment);

        String saName = deployment.getName() + "-sa";
        createServiceAccount(deployment, saName);
        createNetworkPolicy(deployment);

        io.fabric8.kubernetes.api.model.apps.Deployment k8sDeployment = buildDeploymentManifest(deployment, saName);

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

    private io.fabric8.kubernetes.api.model.apps.Deployment buildDeploymentManifest(Deployment deployment, String saName) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(deployment.getName())
                .withNamespace(deployment.getNamespace())
                .addToLabels("deployment-id", deployment.getId().toString())
                .addToLabels("project-id", deployment.getProject() != null ? deployment.getProject().getId().toString() : "none")
                .addToLabels("deployed-by", deployment.getDeployedBy() != null ? deployment.getDeployedBy().getUsername() : "system")
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
                .addToAnnotations("seccomp.security.alpha.kubernetes.io/pod", "runtime/default")
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(saName)
                .withNewSecurityContext()
                    .withRunAsNonRoot(true)
                    .withRunAsUser(10001L) // Must not be 0
                .endSecurityContext()
                .addNewContainer()
                .withName(deployment.getName())
                .withImage(deployment.getImage())
                .withNewSecurityContext()
                    .withAllowPrivilegeEscalation(false)
                    .withReadOnlyRootFilesystem(true)
                    .withNewCapabilities().addToDrop("ALL").endCapabilities()
                .endSecurityContext()
                .addNewPort().withContainerPort(deployment.getPort()).endPort()
                .withNewResources()
                .addToRequests("cpu", new Quantity(deployment.getCpu()))
                .addToRequests("memory", new Quantity(deployment.getMemory()))
                .addToLimits("cpu", new Quantity(deployment.getCpu()))
                .addToLimits("memory", new Quantity(deployment.getMemory()))
                .endResources()
                .withNewLivenessProbe()
                    .withNewTcpSocket().withNewPort(deployment.getPort()).endTcpSocket()
                    .withInitialDelaySeconds(15).withPeriodSeconds(20)
                .endLivenessProbe()
                .withNewReadinessProbe()
                    .withNewTcpSocket().withNewPort(deployment.getPort()).endTcpSocket()
                    .withInitialDelaySeconds(5).withPeriodSeconds(10)
                .endReadinessProbe()
                .withNewStartupProbe()
                    .withNewTcpSocket().withNewPort(deployment.getPort()).endTcpSocket()
                    .withInitialDelaySeconds(5).withPeriodSeconds(10).withFailureThreshold(30)
                .endStartupProbe()
                .addAllToEnv(toEnvVarList(deployment))
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private void createServiceAccount(Deployment deployment, String saName) {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata().withName(saName).withNamespace(deployment.getNamespace()).endMetadata()
                .withAutomountServiceAccountToken(false)
                .build();
        kubernetesClient.serviceAccounts().inNamespace(deployment.getNamespace()).resource(sa).createOr(existing -> existing.update());
    }

    private void createNetworkPolicy(Deployment deployment) {
        NetworkPolicy netPolicy = new NetworkPolicyBuilder()
                .withNewMetadata().withName(deployment.getName() + "-netpol").withNamespace(deployment.getNamespace()).endMetadata()
                .withNewSpec()
                .withNewPodSelector().addToMatchLabels("app", deployment.getName()).endPodSelector()
                .withPolicyTypes("Ingress", "Egress")
                .addNewEgress().endEgress()
                .endSpec()
                .build();
        kubernetesClient.network().networkPolicies().inNamespace(deployment.getNamespace()).resource(netPolicy).createOr(existing -> existing.update());
    }

    private void validateDeploymentConfig(Deployment deployment) {
        if (deployment.getImage() == null || deployment.getImage().isBlank()) {
            throw new IllegalArgumentException("Image is required");
        }
        
        String imagePrefix = deployment.getImage().contains("/") ? deployment.getImage().substring(0, deployment.getImage().indexOf("/")) : "";
        if (!imagePrefix.isEmpty() && !imagePrefix.contains(".")) {
            // Implicit docker.io
            imagePrefix = "docker.io";
        } else if (imagePrefix.isEmpty()) {
            imagePrefix = "docker.io";
        }
        
        String finalPrefix = imagePrefix;
        boolean allowed = ALLOWED_REGISTRIES.stream().anyMatch(registry -> finalPrefix.equalsIgnoreCase(registry) || finalPrefix.endsWith("." + registry));
        if (!allowed) {
            throw new IllegalArgumentException("Image registry not allowed. Allowed registries: " + ALLOWED_REGISTRIES);
        }

        validateResourceLimit(deployment.getCpu(), 2000, "CPU", "m");
        validateResourceLimit(deployment.getMemory(), 4096, "Memory", "Mi");
    }

    private void validateResourceLimit(String value, int max, String name, String suffix) {
        if (value != null && value.endsWith(suffix)) {
            try {
                int val = Integer.parseInt(value.replace(suffix, "").trim());
                if (val > max) {
                    throw new IllegalArgumentException(name + " limit exceeds maximum allowed (" + max + suffix + ")");
                }
            } catch (NumberFormatException ignored) {}
        }
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
        Map<String, String> secretReferences = deployment.getSecretVariables();
        var variables = new java.util.ArrayList<EnvVar>();
        if (envVariables != null) envVariables.forEach((name, value) -> variables.add(new EnvVar(name, value, null)));
        if (secretReferences != null) secretReferences.forEach((environmentName, reference) -> {
            String[] parts = reference.split("/", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) throw new IllegalArgumentException("Reference de secret invalide pour " + environmentName);
            variables.add(new EnvVarBuilder().withName(environmentName).withNewValueFrom().withNewSecretKeyRef().withName(parts[0]).withKey(parts[1]).endSecretKeyRef().endValueFrom().build());
        });
        return variables;
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

    public List<Pod> getPods(Deployment deployment) {
        return kubernetesClient.pods()
                .inNamespace(deployment.getNamespace())
                .withLabel("app", deployment.getName())
                .list()
                .getItems();
    }

    @Override
    public void updateSpec(Deployment deployment) {
        validateDeploymentConfig(deployment);
        String saName = deployment.getName() + "-sa";
        io.fabric8.kubernetes.api.model.apps.Deployment updated = buildDeploymentManifest(deployment, saName);

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

    public void scale(Deployment deployment, int replicas) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .scale(replicas);
    }

    public void stop(Deployment deployment) {
        scale(deployment, 0);
    }

    public void restart(Deployment deployment) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .rolling()
                .restart();
    }

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

    public void delete(Deployment deployment) {
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .delete();

        kubernetesClient.services()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName())
                .delete();
                
        kubernetesClient.serviceAccounts()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName() + "-sa")
                .delete();
                
        kubernetesClient.network().networkPolicies()
                .inNamespace(deployment.getNamespace())
                .withName(deployment.getName() + "-netpol")
                .delete();
    }

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
