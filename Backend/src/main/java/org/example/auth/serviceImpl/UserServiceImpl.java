package org.example.auth.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.example.auth.dto.CreateUserRequest;
import org.example.auth.dto.UserResponse;
import org.example.auth.entity.Role;
import org.example.auth.entity.User;
import org.example.auth.enums.RoleName;
import org.example.auth.exception.DuplicateResourceException;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.UserRepository;
import org.example.auth.service.RoleService;
import org.example.auth.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());
        validateUniqueIdentity(username, email);

        User user = new User(username, email, passwordEncoder.encode(request.password()));
        roleService.getByNames(request.roles()).forEach(user::addRole);

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllWithRoles().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return toResponse(findByUsername(username));
    }

    @Override
    public UserResponse setEnabled(UUID userId, boolean enabled) {
        User user = findById(userId);
        user.setEnabled(enabled);
        return toResponse(user);
    }

    @Override
    public UserResponse updateRoles(UUID userId, Set<RoleName> roles) {
        User user = findById(userId);
        user.replaceRoles(roleService.getByNames(roles));
        return toResponse(user);
    }

    @Override
    public void recordSuccessfulLogin(String username) {
        findByUsername(username).recordSuccessfulLogin();
    }

    private User findById(UUID userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable"
                ));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(normalize(username))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable"
                ));
    }

    private void validateUniqueIdentity(String username, String email) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Ce nom d'utilisateur existe déjà");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Cette adresse email existe déjà");
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        Set<RoleName> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getLastLogin(),
                roles
        );
    }
}
