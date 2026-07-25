package org.example.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;
import org.example.History.Service.IActionHistoryService;
import org.example.auth.dto.CreateUserRequest;
import org.example.auth.dto.UpdateUserRolesRequest;
import org.example.auth.dto.UpdateUserStatusRequest;
import org.example.auth.dto.UserResponse;
import org.example.auth.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final IActionHistoryService historyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        UserResponse user = userService.createUser(request);
        record(ActionType.CREATE, "Création de l'utilisateur", user.username(), authentication, httpRequest);
        return user;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.getAllUsers();
    }

    @PatchMapping("/{userId}/enabled")
    public UserResponse setEnabled(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        UserResponse user = userService.setEnabled(userId, request.enabled());
        record(
                ActionType.UPDATE,
                request.enabled() ? "Activation de l'utilisateur" : "Désactivation de l'utilisateur",
                user.username(),
                authentication,
                httpRequest
        );
        return user;
    }

    @PutMapping("/{userId}/roles")
    public UserResponse updateRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRolesRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        UserResponse user = userService.updateRoles(userId, request.roles());
        record(ActionType.UPDATE, "Modification des rôles utilisateur", user.username(), authentication, httpRequest);
        return user;
    }

    private void record(
            ActionType action,
            String details,
            String targetUsername,
            Authentication authentication,
            HttpServletRequest request
    ) {
        historyService.record(
                action,
                details,
                TargetType.USER,
                targetUsername,
                authentication.getName(),
                request.getRemoteAddr()
        );
    }
}
