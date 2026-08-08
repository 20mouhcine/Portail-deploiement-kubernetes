package org.example.Projects.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Projects.Entity.Project;

import java.util.UUID;
import java.util.Set;

/**
 * @author pc
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {

    private UUID id;

    private UUID ownerId;

    private String ownerUsername;

    private String name;

    private String description;

    private String repository;

    private Set<String> allowedNamespaces;
    private Set<String> allowedUsers;
    private String environmentType;
    private String deploymentPolicy;
    private String cpuQuota;
    private String memoryQuota;
    private Integer podQuota;


    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOwner() != null ? project.getOwner().getId() : null,
                project.getOwner() != null ? project.getOwner().getUsername() : null,
                project.getName(),
                project.getDescription(),
                project.getRepository(),
                Set.copyOf(project.getAllowedNamespaces()),
                project.getAllowedUsers().stream().map(user -> user.getUsername()).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                project.getEnvironmentType(),
                project.getDeploymentPolicy(),
                project.getCpuQuota(),
                project.getMemoryQuota(),
                project.getPodQuota()
        );
    }
}
