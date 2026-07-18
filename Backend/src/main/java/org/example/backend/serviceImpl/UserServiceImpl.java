package org.example.backend.serviceimpl;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.CreateUserRequest;
import org.example.backend.dto.UserResponse;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.enums.RoleName;
import org.example.backend.exception.DuplicateResourceException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.RoleService;
import org.example.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User user = findById(userId);
        user.setEnabled(enabled);
        return toResponse(user);
    }

    @Override
    public UserResponse updateRoles(Long userId, Set<RoleName> roles) {
        User user = findById(userId);
        user.replaceRoles(roleService.getByNames(roles));
        return toResponse(user);
    }

    @Override
    public void recordSuccessfulLogin(String username) {
        findByUsername(username).recordSuccessfulLogin();
    }

    private User findById(Long userId) {
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
