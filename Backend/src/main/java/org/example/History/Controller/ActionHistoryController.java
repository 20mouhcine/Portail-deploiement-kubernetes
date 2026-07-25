package org.example.History.Controller;

import lombok.RequiredArgsConstructor;
import org.example.History.DTO.ActionHistoryResponse;
import org.example.History.Service.IActionHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/action-history")
@RequiredArgsConstructor
public class ActionHistoryController {

    private final IActionHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<ActionHistoryResponse>> findAll(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(historyService.findVisibleHistory(
                authentication.getName(),
                admin
        ));
    }
}
