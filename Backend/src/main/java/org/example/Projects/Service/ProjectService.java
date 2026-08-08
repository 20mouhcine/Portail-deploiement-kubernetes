package org.example.Projects.Service;

import lombok.RequiredArgsConstructor;
import org.example.Projects.DTO.CreateProjectRequest;
import org.example.Projects.DTO.ProjectResponse;
import org.example.Projects.DTO.UpdateProjectRequest;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.example.Projects.Security.ProjectAccessService;
import org.example.auth.entity.User;
import org.example.auth.exception.DuplicateResourceException;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService implements IProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getApplications(Authentication authentication) {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(project -> projectAccessService.canRead(project, authentication))
                .map(ProjectResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Project getApplicationByName(String name) {
        return projectRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getApplicationById(UUID id) {
        return projectRepository.findWithAccessById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    @Override
    public ProjectResponse createApplication(CreateProjectRequest request, Authentication authentication) {
        String name = request.getName().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Un projet portant ce nom existe déjà");
        }
        User owner = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable"));
        Project project = new Project();
        project.setName(name);
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());
        apply(project, request.getDescription(), request.getRepository(), request.getAllowedNamespaces(),
                request.getAllowedUsers(), request.getEnvironmentType(), request.getDeploymentPolicy(),
                request.getCpuQuota(), request.getMemoryQuota(), request.getPodQuota());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    public void deleteApplicationById(UUID id) {
        projectRepository.delete(getApplicationById(id));
    }

    @Override
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = getApplicationById(id);
        String name = request.getName().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Un projet portant ce nom existe déjà");
        }
        project.setName(name);
        apply(project, request.getDescription(), request.getRepository(), request.getAllowedNamespaces(),
                request.getAllowedUsers(), request.getEnvironmentType(), request.getDeploymentPolicy(),
                request.getCpuQuota(), request.getMemoryQuota(), request.getPodQuota());
        return ProjectResponse.from(projectRepository.save(project));
    }

    private void apply(Project project, String description, String repository, Set<String> namespaces,
                       Set<String> usernames, String environmentType, String deploymentPolicy,
                       String cpuQuota, String memoryQuota, Integer podQuota) {
        project.setDescription(description == null ? null : description.trim());
        project.setRepository(repository.trim());
        project.setAllowedNamespaces(normalizeNamespaces(namespaces));
        project.setAllowedUsers(resolveUsers(usernames));
        project.setEnvironmentType(trimToNull(environmentType));
        project.setDeploymentPolicy(trimToNull(deploymentPolicy));
        project.setCpuQuota(trimToNull(cpuQuota));
        project.setMemoryQuota(trimToNull(memoryQuota));
        project.setPodQuota(podQuota);
    }

    private Set<String> normalizeNamespaces(Set<String> namespaces) {
        if (namespaces == null || namespaces.isEmpty()) return new LinkedHashSet<>(Set.of("default"));
        Set<String> normalized = new LinkedHashSet<>();
        namespaces.forEach(value -> normalized.add(value.trim().toLowerCase()));
        return normalized;
    }

    private Set<User> resolveUsers(Set<String> usernames) {
        Set<User> users = new LinkedHashSet<>();
        if (usernames == null) return users;
        usernames.forEach(username -> users.add(userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur autorisé introuvable: " + username))));
        return users;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
