package org.example.backend;

import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;
import org.example.History.Repository.ActionHistoryRepository;
import org.example.History.Service.IActionHistoryService;
import org.example.auth.entity.Role;
import org.example.auth.entity.User;
import org.example.auth.enums.RoleName;
import org.example.auth.repository.RoleRepository;
import org.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActionHistoryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IActionHistoryService historyService;

    @Autowired
    private ActionHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanData() {
        historyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void historyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/action-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void developerSeesOnlyOwnHistory() throws Exception {
        createUser("developer", RoleName.DEVELOPER);
        createUser("other", RoleName.DEVOPS);
        recordFor("developer", "Projet du développeur");
        recordFor("other", "Projet d'un autre utilisateur");

        mockMvc.perform(get("/api/action-history")
                        .with(user("developer").roles("DEVELOPER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("developer"))
                .andExpect(jsonPath("$[0].targetName").value("Projet du développeur"));
    }

    @Test
    void adminSeesAllHistory() throws Exception {
        createUser("admin", RoleName.ADMIN);
        createUser("devops", RoleName.DEVOPS);
        recordFor("admin", "Administration");
        recordFor("devops", "Déploiement");

        mockMvc.perform(get("/api/action-history")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private void recordFor(String username, String targetName) {
        historyService.record(
                ActionType.CREATE,
                "Création de test",
                TargetType.PROJECT,
                targetName,
                username,
                "127.0.0.1"
        );
    }

    private User createUser(String username, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User(
                username,
                username + "@example.com",
                passwordEncoder.encode("CorrectPassword123!")
        );
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }
}
