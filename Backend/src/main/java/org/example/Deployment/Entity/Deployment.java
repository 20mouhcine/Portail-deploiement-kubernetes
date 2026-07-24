package org.example.Deployment.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Projects.Entity.Project;
import org.example.auth.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
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

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = DeploymentStatus.PENDING;
        }
    }
}
