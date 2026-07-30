package org.example.Deployment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Deployment.Entity.DeploymentEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentEventResponse {

    private UUID id;
    private LocalDateTime timestamp;
    private String level;
    private String source;
    private String message;

    public static DeploymentEventResponse from(DeploymentEvent event) {
        return new DeploymentEventResponse(
                event.getId(),
                event.getTimestamp(),
                event.getLevel().name(),
                event.getSource().name(),
                event.getMessage()
        );
    }
}