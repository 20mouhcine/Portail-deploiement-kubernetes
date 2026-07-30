package org.example.Deployment.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deployment_revisions")
public class DeploymentRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "deployment_id", nullable = false)
    private UUID deploymentId;

    @Column(nullable = false)
    private Integer revisionNumber;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private Integer replicas;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, length = 20)
    private String cpu;

    @Column(nullable = false, length = 20)
    private String memory;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deployment_revision_env_variables", joinColumns = @JoinColumn(name = "revision_id"))
    @MapKeyColumn(name = "env_key")
    @Column(name = "env_value")
    private Map<String, String> envVariables = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deployment_revision_secret_variables", joinColumns = @JoinColumn(name = "revision_id"))
    @MapKeyColumn(name = "secret_key")
    @Column(name = "secret_value")
    private Map<String, String> secretVariables = new HashMap<>();

    @Column(name = "git_repository")
    private String gitRepository;

    @Column(name = "git_branch")
    private String gitBranch;

    @Column(name = "git_commit")
    private String gitCommit;

    @Column(name = "git_tag")
    private String gitTag;

    @Column(name = "requested_hostname")
    private String requestedHostname;

    @Column(name = "requested_path")
    private String requestedPath;

    @Column(name = "tls_enabled")
    private Boolean tlsEnabled = Boolean.FALSE;

    @Column(name = "tls_secret_name")
    private String tlsSecretName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}