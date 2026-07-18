package org.example.backend;

import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.enums.RoleName;
import org.example.backend.repository.RoleRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    void csrfEndpointIsPublicAndCreatesCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Authentification requise"));
    }

    @Test
    void validCredentialsCreateSessionAndExposeCurrentUser() throws Exception {
        createUser("admin", "admin@example.com", "CorrectPassword123!", RoleName.ADMIN);

        MvcResult loginResult = login("admin", "CorrectPassword123!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Connexion réussie"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("ADMIN")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidCredentialsAreRejectedWithGenericMessage() throws Exception {
        createUser("admin", "admin@example.com", "CorrectPassword123!", RoleName.ADMIN);

        login("admin", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants incorrects"));
    }

    @Test
    void repeatedLoginFailuresAreRateLimited() throws Exception {
        createUser("rate-limited", "rate-limited@example.com", "CorrectPassword123!", RoleName.DEVELOPER);

        for (int attempt = 0; attempt < 5; attempt++) {
            login("rate-limited", "wrong-password")
                    .andExpect(status().isUnauthorized());
        }

        login("rate-limited", "wrong-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Trop de tentatives. Réessayez plus tard."))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
    }

    @Test
    void loginRequiresCsrfProtection() throws Exception {
        createUser("admin", "admin@example.com", "CorrectPassword123!", RoleName.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "CorrectPassword123!"))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        User user = createUser(
                "disabled",
                "disabled@example.com",
                "CorrectPassword123!",
                RoleName.DEVELOPER
        );
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        login("disabled", "CorrectPassword123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants incorrects"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        createUser("admin", "admin@example.com", "CorrectPassword123!", RoleName.ADMIN);
        MockHttpSession session = authenticatedSession("admin", "CorrectPassword123!");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Déconnexion réussie"));

        assertTrue(session.isInvalid());
    }

    @Test
    void developerCannotCreateUsers() throws Exception {
        createUser("developer", "developer@example.com", "CorrectPassword123!", RoleName.DEVELOPER);
        MockHttpSession session = authenticatedSession("developer", "CorrectPassword123!");

        mockMvc.perform(post("/api/admin/users")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "other",
                                  "email": "other@example.com",
                                  "password": "AnotherPassword123!",
                                  "roles": ["DEVELOPER"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void adminCanCreateUserWithoutExposingPasswordHash() throws Exception {
        createUser("admin", "admin@example.com", "CorrectPassword123!", RoleName.ADMIN);
        MockHttpSession session = authenticatedSession("admin", "CorrectPassword123!");

        mockMvc.perform(post("/api/admin/users")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new.dev",
                                  "email": "new.dev@example.com",
                                  "password": "AnotherPassword123!",
                                  "roles": ["DEVELOPER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new.dev"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("DEVELOPER")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String username,
            String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username)
                .param("password", password));
    }

    private MockHttpSession authenticatedSession(
            String username,
            String password) throws Exception {
        MvcResult result = login(username, password)
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private User createUser(
            String username,
            String email,
            String password,
            RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User(username, email, passwordEncoder.encode(password));
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }
}
