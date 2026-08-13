package org.example.History.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.History.DTO.ActionHistoryResponse;
import org.example.History.Entity.ActionHistory;
import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;
import org.example.History.Repository.ActionHistoryRepository;
import org.example.History.Service.IActionHistoryService;
import org.example.auth.entity.User;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionHistoryServiceImpl implements IActionHistoryService {

    private final ActionHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ActionHistoryResponse> findVisibleHistory(String username, boolean admin) {
        List<ActionHistory> entries = admin
                ? historyRepository.findAllByOrderByCreatedAtDesc()
                : historyRepository.findByUserUsernameIgnoreCaseOrderByCreatedAtDesc(username);
        return entries.stream().map(ActionHistoryResponse::from).toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            ActionType action,
            String details,
            TargetType targetType,
            String targetName,
            String username,
            String ipAddress
    ) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable pour l'enregistrement de l'historique"
                ));


        ActionHistory history = ActionHistory.create();
        history.setAction(action);
        history.setDetails(limit(details, 1000));
        history.setTargetType(targetType);
        history.setTargetName(limit(targetName, 255));
        history.setIpAddress(limit(ipAddress == null ? "unknown" : ipAddress, 45));
        history.setUser(user);
        log.info("historique: {}", history);
        historyRepository.save(history);
    }

    private String limit(String value, int maximumLength) {
        String safeValue = value == null || value.isBlank() ? "Non renseigné" : value.trim();
        return safeValue.length() <= maximumLength
                ? safeValue
                : safeValue.substring(0, maximumLength);
    }
}
