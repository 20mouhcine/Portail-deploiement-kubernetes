package org.example.Projects.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.Projects.Entity.Project;

import java.util.UUID;

/**
 * @author pc
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {

    private UUID id;

    private String name;

    private String description;

    private String repository;


    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getRepository()
        );
    }
}
