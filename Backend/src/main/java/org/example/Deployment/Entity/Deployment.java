package org.example.Deployment.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Projects.Entity.Project;
import org.example.auth.entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "deployments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deployment_name_namespace",
                columnNames = {"name", "namespace"}
        )
)
public class Deployment {

    public static Deployment create() {
        return new Deployment();
    }

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentStatus status = DeploymentStatus.PENDING;

    @Column(nullable = false, length = 63)
    private String namespace;

    @Column(nullable = false)
    private Integer replicas;

    @Column(nullable = false, length = 255)
    private String image;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, length = 20)
    private String cpu;

    @Column(nullable = false, length = 20)
    private String memory;

    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployed_by", nullable = false)
    private User deployedBy;

    @Column(name = "access_url")
    private String accessUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deployment_env_variables", joinColumns = @JoinColumn(name = "deployment_id"))
    @MapKeyColumn(name = "env_key")
    @Column(name = "env_value")
    private Map<String, String> envVariables = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deployment_secret_variables", joinColumns = @JoinColumn(name = "deployment_id"))
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

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = DeploymentStatus.PENDING;
        }
    }
}
