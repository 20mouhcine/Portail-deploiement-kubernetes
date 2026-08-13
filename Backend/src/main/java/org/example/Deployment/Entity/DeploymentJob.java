package org.example.Deployment.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.Deployment.Enums.JobOperationType;
import org.example.Deployment.Enums.JobStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployment_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentJob {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID deploymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    // e.g. store replicas for scale operation
    private Integer targetReplicas;

    private LocalDateTime nextAttemptAt;

    private LocalDateTime startedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    @Column(columnDefinition = "bigint default 0", nullable = false)
    private long version;
}
