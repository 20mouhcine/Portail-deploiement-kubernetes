package org.example.Projects.Service;

import lombok.AllArgsConstructor;
import org.example.Projects.Controller.ProjectController;
import org.example.Projects.DTO.ProjectResponse;
import org.example.Projects.DTO.CreateProjectRequest;
import org.example.Projects.DTO.UpdateProjectRequest;
import org.example.Projects.Entity.Project;
import org.example.Projects.Exceptions.ProjectAlreadyExistsException;
import org.example.Projects.Repository.ProjectRepository;
import org.example.auth.entity.User;
import org.example.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@Service
@AllArgsConstructor
public class ProjectService implements IProjectService {
   private final ProjectRepository projectRepository;
   private final UserRepository userRepository;
    @Override
    public List<ProjectResponse> getApplications() {

        return projectRepository.findAll()
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Override
    public Project getApplicationByName(String name) {
        return projectRepository.findByName(name);
    }

    @Override
    public Project getApplicationById(UUID id) {
        return projectRepository.findById(id).orElse(null);
    }

    @Override
    public ProjectResponse createApplication(CreateProjectRequest request) {
        if(projectRepository.existsByName(request.getName())){
            throw new ProjectAlreadyExistsException(
                    "Application " + request.getName() + " already exists."
            );
        }

        User owner = userRepository.findById(request.getOwner_id())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getOwner_id()));

        Project app = new Project();

        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setRepository(request.getRepository());
        app.setOwner(owner);

        app.setCreatedAt(LocalDateTime.now());

        Project saved = projectRepository.save(app);

        return ProjectResponse.from(saved);
    }
@Override
    public void deleteApplicationById(UUID id){
        projectRepository.deleteById(id);
}

    @Override
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {

        Project project = projectRepository.findById(id).orElse(null);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setRepository(request.getRepository());
        Project saved = projectRepository.save(project);

        return ProjectResponse.from(saved);

    }
}
