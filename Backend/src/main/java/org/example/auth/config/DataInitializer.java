package org.example.auth.config;

import org.example.auth.dto.CreateUserRequest;
import org.example.auth.entity.Role;
import org.example.auth.enums.RoleName;
import org.example.auth.repository.RoleRepository;
import org.example.auth.repository.UserRepository;
import org.example.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.bootstrap-admin.username:}")
    private String adminUsername;

    @Value("${app.bootstrap-admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;

    @Bean
    CommandLineRunner initializeSecurityData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            UserService userService) {
        return args -> {
            upsertRole(roleRepository, RoleName.ADMIN, "Administration complète du portail");
            upsertRole(roleRepository, RoleName.DEVOPS, "Gestion des déploiements Kubernetes");
            upsertRole(roleRepository, RoleName.DEVELOPER, "Accès développeur aux applications");

            createInitialAdminIfConfigured(userRepository, userService);
        };
    }

    private void upsertRole(
            RoleRepository roleRepository,
            RoleName roleName,
            String description) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> new Role(roleName, description));
        role.setDescription(description);
        roleRepository.save(role);
    }

    private void createInitialAdminIfConfigured(
            UserRepository userRepository,
            UserService userService) {
        boolean noConfiguration = adminUsername.isBlank()
                && adminEmail.isBlank()
                && adminPassword.isBlank();
        if (noConfiguration) {
            LOGGER.info("Aucun administrateur initial configuré");
            return;
        }

        if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_USERNAME, INITIAL_ADMIN_EMAIL et INITIAL_ADMIN_PASSWORD "
                            + "doivent être configurés ensemble"
            );
        }

        if (adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD doit contenir au moins 12 caractères"
            );
        }

        if (userRepository.existsByUsernameIgnoreCase(adminUsername.trim())) {
            LOGGER.info("L'administrateur initial existe déjà");
            return;
        }

        userService.createUser(new CreateUserRequest(
                adminUsername,
                adminEmail,
                adminPassword,
                Set.of(RoleName.ADMIN)
        ));
        LOGGER.info("Administrateur initial créé avec succès");
    }
}
