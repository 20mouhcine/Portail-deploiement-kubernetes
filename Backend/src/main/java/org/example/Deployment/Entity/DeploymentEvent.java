package org.example.Deployment.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.Deployment.Enums.DeploymentEventLevel;
import org.example.Deployment.Enums.DeploymentEventSource;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deployment_events")
public class DeploymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "deployment_id", nullable = false)
    private UUID deploymentId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentEventLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentEventSource source;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @PrePersist
    void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
