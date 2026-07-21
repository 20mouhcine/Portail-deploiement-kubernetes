package org.example.auth.service;

import org.example.auth.dto.CreateUserRequest;
import org.example.auth.dto.UserResponse;
import org.example.auth.enums.RoleName;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getByUsername(String username);

    UserResponse setEnabled(UUID userId, boolean enabled);

    UserResponse updateRoles(UUID userId, Set<RoleName> roles);

    void recordSuccessfulLogin(String username);
}
