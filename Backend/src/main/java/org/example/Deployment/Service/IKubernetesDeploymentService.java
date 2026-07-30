package org.example.Deployment.Service;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import org.example.Deployment.Entity.Deployment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author pc
 **/
public interface IKubernetesDeploymentService {
    String deploy(Deployment deployment) throws InterruptedException ;

    String getAccessUrl(Deployment deployment);

    public String getStatus(Deployment deployment);
    public List<Pod> getPods(Deployment deployment);
    public void scale(Deployment deployment, int replicas);
    public void stop(Deployment deployment);
    public void restart(Deployment deployment);
    public void updateImage(Deployment deployment, String newImage);
    public void delete(Deployment deployment);
    public String getLogs(Deployment deployment);

    private List<EnvVar> toEnvVarList(Map<String, String> envVariables){
        if (envVariables == null) return List.of();
        return envVariables.entrySet().stream()
                .map(e -> new EnvVar(e.getKey(), e.getValue(), null))
                .collect(Collectors.toList());
    }

    void updateSpec(Deployment saved);
}
