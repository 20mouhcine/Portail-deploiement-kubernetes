package org.example.Deployment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Deployment.Entity.DeploymentRevision;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRevisionResponse {

    private UUID id;
    private Integer revisionNumber;
    private String image;
    private Integer replicas;
    private Integer port;
    private String cpu;
    private String memory;
    private String gitCommit;
    private String gitTag;
    private LocalDateTime createdAt;
    private Map<String, String> envVariables;
    private Map<String, String> secretVariables;
    private String requestedHostname;
    private String requestedPath;
    private Boolean tlsEnabled;
    private String tlsSecretName;

    public static DeploymentRevisionResponse from(DeploymentRevision revision) {
        return new DeploymentRevisionResponse(
                revision.getId(),
                revision.getRevisionNumber(),
                revision.getImage(),
                revision.getReplicas(),
                revision.getPort(),
                revision.getCpu(),
                revision.getMemory(),
                revision.getGitCommit(),
                revision.getGitTag(),
                revision.getCreatedAt(),
                revision.getEnvVariables(),
                revision.getSecretVariables(),
                revision.getRequestedHostname(),
                revision.getRequestedPath(),
                revision.getTlsEnabled(),
                revision.getTlsSecretName()
        );
    }
}