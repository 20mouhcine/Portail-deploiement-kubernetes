package org.example.Projects.Service;

import org.example.Projects.DTO.ProjectResponse;
import org.example.Projects.DTO.CreateProjectRequest;
import org.example.Projects.DTO.UpdateProjectRequest;
import org.example.Projects.Entity.Project;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@Service
public interface IProjectService {
    List<ProjectResponse> getApplications(Authentication authentication);
    Project getApplicationByName(String name);
    Project getApplicationById(UUID id);
    ProjectResponse createApplication(CreateProjectRequest request, Authentication authentication);

    void deleteApplicationById(UUID id);

    ProjectResponse update(UUID id, UpdateProjectRequest request);
}
