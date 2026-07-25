package org.example.History.Service;

import org.example.History.DTO.ActionHistoryResponse;
import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;

import java.util.List;

public interface IActionHistoryService {

    List<ActionHistoryResponse> findVisibleHistory(String username, boolean admin);

    void record(
            ActionType action,
            String details,
            TargetType targetType,
            String targetName,
            String username,
            String ipAddress
    );
}
