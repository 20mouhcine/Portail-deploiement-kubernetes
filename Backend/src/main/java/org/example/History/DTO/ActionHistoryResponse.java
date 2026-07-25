package org.example.History.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.History.Entity.ActionHistory;
import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionHistoryResponse {

    private UUID id;
    private ActionType action;
    private String details;
    private LocalDateTime createdAt;
    private String ipAddress;
    private String username;
    private TargetType targetType;
    private String targetName;

    public static ActionHistoryResponse from(ActionHistory history) {
        return new ActionHistoryResponse(
                history.getId(),
                history.getAction(),
                history.getDetails(),
                history.getCreatedAt(),
                history.getIpAddress(),
                history.getUser().getUsername(),
                history.getTargetType(),
                history.getTargetName()
        );
    }
}
