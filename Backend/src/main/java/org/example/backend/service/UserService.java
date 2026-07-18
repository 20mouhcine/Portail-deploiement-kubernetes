package org.example.backend.service;

import org.example.backend.dto.CreateUserRequest;
import org.example.backend.dto.UserResponse;
import org.example.backend.enums.RoleName;

import java.util.List;
import java.util.Set;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getByUsername(String username);

    UserResponse setEnabled(Long userId, boolean enabled);

    UserResponse updateRoles(Long userId, Set<RoleName> roles);

    void recordSuccessfulLogin(String username);
}
