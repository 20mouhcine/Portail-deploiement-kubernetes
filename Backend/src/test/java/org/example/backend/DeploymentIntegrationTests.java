package org.example.backend;

import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.example.auth.entity.Role;
import org.example.auth.entity.User;
import org.example.auth.enums.RoleName;
import org.example.auth.repository.RoleRepository;
import org.example.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeploymentJobRepository deploymentJobRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanData() {
        deploymentJobRepository.deleteAll();
        deploymentRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void devopsCanCreateDeploymentConfiguration() throws Exception {
        User devops = createUser("devops", RoleName.DEVOPS);
        Project project = createProject("Portail Kubernetes", devops);

        mockMvc.perform(post("/api/deployments")
                        .with(user("devops").roles("DEVOPS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "name": "backend-api",
                                  "namespace": "production",
                                  "replicas": 3,
                                  "image": "kube-portal/backend:1.0.0",
                                  "port": 8080,
                                  "cpu": "500m",
                                  "memory": "512Mi"
                                }
                                """.formatted(project.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("backend-api"))
                .andExpect(jsonPath("$.data.projectName").value("Portail Kubernetes"))
                .andExpect(jsonPath("$.data.deployedBy").value("devops"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void adminCanCreateDeploymentForAnExistingProject() throws Exception {
        User owner = createUser("admin-project-owner", RoleName.DEVOPS);
        createUser("admin-deployer", RoleName.ADMIN);
        Project project = createProject("Admin deployment project", owner);

        mockMvc.perform(post("/api/deployments")
                        .with(user("admin-deployer").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "name": "backend-api",
                                  "namespace": "production",
                                  "replicas": 2,
                                  "image": "kube-portal/backend:1.0.0",
                                  "port": 8080,
                                  "cpu": "500m",
                                  "memory": "512Mi"
                                }
                                """.formatted(project.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("backend-api"))
                .andExpect(jsonPath("$.data.deployedBy").value("admin-deployer"));
    }

    @Test
    void developerCanReadDeployments() throws Exception {
        mockMvc.perform(get("/api/deployments")
                        .with(user("developer").roles("DEVELOPER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminCanListExistingDeployment() throws Exception {
        User owner = createUser("list-owner", RoleName.DEVOPS);
        createUser("list-admin", RoleName.ADMIN);
        Deployment deployment = createDeployment(createProject("List project", owner), owner);

        mockMvc.perform(get("/api/deployments")
                        .with(user("list-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deployment.getId().toString()))
                .andExpect(jsonPath("$[0].projectName").value("List project"))
                .andExpect(jsonPath("$[0].deployedBy").value("list-owner"));
    }

    @Test
    void projectOwnerComesFromAuthenticatedUser() throws Exception {
        User creator = createUser("creator", RoleName.DEVELOPER);
        User victim = createUser("victim", RoleName.DEVELOPER);

        mockMvc.perform(post("/api/projects")
                        .with(user("creator").roles("DEVELOPER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Secure Project",
                                  "description": "test",
                                  "repository": "https://github.com/example/secure",
                                  "owner_id": "%s",
                                  "allowedNamespaces": ["default"]
                                }
                                """.formatted(victim.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownerUsername").value("creator"));

        Project saved = projectRepository.findByName("Secure Project");
        org.assertj.core.api.Assertions.assertThat(saved.getOwner().getId()).isEqualTo(creator.getId());
    }

    @Test
    void unrelatedDeveloperCannotReadDeploymentDetails() throws Exception {
        User owner = createUser("owner", RoleName.DEVOPS);
        createUser("unrelated", RoleName.DEVELOPER);
        Deployment deployment = createDeployment(createProject("Private", owner), owner);

        mockMvc.perform(get("/api/deployments/{id}/details", deployment.getId())
                        .with(user("unrelated").roles("DEVELOPER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void invalidReplicaCountIsRejected() throws Exception {
        mockMvc.perform(post("/api/deployments")
                        .with(user("devops").roles("DEVOPS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "00000000-0000-0000-0000-000000000001",
                                  "name": "backend-api",
                                  "namespace": "production",
                                  "replicas": 0,
                                  "image": "kube-portal/backend:1.0.0",
                                  "port": 8080,
                                  "cpu": "500m",
                                  "memory": "512Mi"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.replicas").exists());
    }

    @Test
    void adminCanStopDeployment() throws Exception {
        User devops = createUser("devops", RoleName.DEVOPS);
        createUser("admin", RoleName.ADMIN);
        Project project = createProject("Portail Kubernetes", devops);
        Deployment deployment = createDeployment(project, devops);

        mockMvc.perform(patch("/api/deployments/{id}/stop", deployment.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STOPPED"));
    }

    @Test
    void jobQueryLoadsEnvironmentAndSecretVariablesOutsideTheRepositorySession() {
        User devops = createUser("job-owner", RoleName.DEVOPS);
        Deployment deployment = createDeployment(createProject("Job project", devops), devops);
        deployment.setEnvVariables(Map.of("APP_MODE", "test"));
        deployment.setSecretVariables(Map.of("API_TOKEN", "app-secret/token"));
        deployment = deploymentRepository.saveAndFlush(deployment);

        Deployment loaded = deploymentRepository.findForJobById(deployment.getId()).orElseThrow();

        assertThat(loaded.getEnvVariables()).containsEntry("APP_MODE", "test");
        assertThat(loaded.getSecretVariables()).containsEntry("API_TOKEN", "app-secret/token");
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

    private Project createProject(String name, User owner) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Projet de test");
        project.setRepository("https://github.com/example/test");
        project.setCreatedAt(LocalDateTime.now());
        project.setOwner(owner);
        project.setAllowedNamespaces(Set.of("production"));
        return projectRepository.saveAndFlush(project);
    }

    private Deployment createDeployment(Project project, User deployedBy) {
        Deployment deployment = Deployment.create();
        deployment.setName("backend-api");
        deployment.setNamespace("production");
        deployment.setReplicas(2);
        deployment.setImage("kube-portal/backend:1.0.0");
        deployment.setPort(8080);
        deployment.setCpu("500m");
        deployment.setMemory("512Mi");
        deployment.setStatus(DeploymentStatus.RUNNING);
        deployment.setProject(project);
        deployment.setDeployedBy(deployedBy);
        return deploymentRepository.saveAndFlush(deployment);
    }
}
