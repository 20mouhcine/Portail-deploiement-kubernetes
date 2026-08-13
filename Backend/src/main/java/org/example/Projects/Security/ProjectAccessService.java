package org.example.Projects.Security;

import lombok.RequiredArgsConstructor;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component("projectAccess")
@RequiredArgsConstructor
public class ProjectAccessService {
    private static final Set<String> PROTECTED_NAMESPACES = Set.of(
            "kube-system", "kube-public", "kube-node-lease"
    );

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentJobRepository deploymentJobRepository;

    public boolean canRead(UUID projectId, Authentication authentication) {
        return projectRepository.findWithAccessById(projectId)
                .map(project -> canRead(project, authentication))
                .orElse(false);
    }

    public boolean canRead(Project project, Authentication authentication) {
        if (!authenticated(authentication)) return false;
        if (hasRole(authentication, "ADMIN")) return true;
        String username = authentication.getName();
        return project.getOwner() != null && project.getOwner().getUsername().equalsIgnoreCase(username)
                || project.getAllowedUsers().stream()
                .anyMatch(user -> user.isEnabled() && user.getUsername().equalsIgnoreCase(username));
    }

    public boolean canManage(UUID projectId, Authentication authentication) {
        if (!authenticated(authentication)) return false;
        if (hasRole(authentication, "ADMIN")) return true;
        return projectRepository.findWithAccessById(projectId)
                .map(project -> project.getOwner() != null
                        && project.getOwner().getUsername().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canDeploy(UUID projectId, String namespace, Authentication authentication) {
        if (!isOperational(authentication) || namespace == null || PROTECTED_NAMESPACES.contains(namespace)) {
            return false;
        }
        return projectRepository.findWithAccessById(projectId)
                .filter(project -> canRead(project, authentication))
                .map(project -> namespaceAllowed(project, namespace))
                .orElse(false);
    }

    public boolean canReadDeployment(UUID deploymentId, Authentication authentication) {
        return deploymentRepository.findWithProjectAccessById(deploymentId)
                .map(deployment -> canRead(deployment.getProject(), authentication))
                .orElse(false);
    }

    public boolean canOperateDeployment(UUID deploymentId, Authentication authentication) {
        return isOperational(authentication) && canReadDeployment(deploymentId, authentication);
    }

    public boolean canReadOperation(UUID jobId, Authentication authentication) {
        if (!authenticated(authentication)) return false;
        if (hasRole(authentication, "ADMIN")) return true;
        return deploymentJobRepository.findById(jobId)
                .map(job -> canReadDeployment(job.getDeploymentId(), authentication))
                .orElse(false);
    }

    private boolean namespaceAllowed(Project project, String namespace) {
        Set<String> allowed = project.getAllowedNamespaces();
        return allowed == null || allowed.isEmpty()
                ? "default".equals(namespace)
                : allowed.contains(namespace);
    }

    private boolean isOperational(Authentication authentication) {
        return authenticated(authentication)
                && (hasRole(authentication, "ADMIN") || hasRole(authentication, "DEVOPS"));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private boolean authenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
