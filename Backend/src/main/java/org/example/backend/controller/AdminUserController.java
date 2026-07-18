package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.CreateUserRequest;
import org.example.backend.dto.UpdateUserRolesRequest;
import org.example.backend.dto.UpdateUserStatusRequest;
import org.example.backend.dto.UserResponse;
import org.example.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.getAllUsers();
    }

    @PatchMapping("/{userId}/enabled")
    public UserResponse setEnabled(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.setEnabled(userId, request.enabled());
    }

    @PutMapping("/{userId}/roles")
    public UserResponse updateRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return userService.updateRoles(userId, request.roles());
    }
}
