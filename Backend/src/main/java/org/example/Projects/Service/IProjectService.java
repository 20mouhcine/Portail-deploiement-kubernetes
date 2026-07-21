package org.example.Projects.Service;

import org.example.Projects.DTO.ProjectResponse;
import org.example.Projects.DTO.CreateProjectRequest;
import org.example.Projects.DTO.UpdateProjectRequest;
import org.example.Projects.Entity.Project;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@Service
public interface IProjectService {
    List<ProjectResponse> getApplications();
    Project getApplicationByName(String name);
    Project getApplicationById(UUID id);
    ProjectResponse createApplication(CreateProjectRequest request);

    void deleteApplicationById(UUID id);

    ProjectResponse update(UUID id,UpdateProjectRequest request);
}
