package org.example.Projects.Controller;

import lombok.AllArgsConstructor;
import org.example.Projects.DTO.ApiResponse;
import org.example.Projects.DTO.ProjectResponse;
import org.example.Projects.DTO.CreateProjectRequest;
import org.example.Projects.DTO.UpdateProjectRequest;
import org.example.Projects.Service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author pc
 **/
@RestController
@RequestMapping("/api/projects")
@AllArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getApplications() {
        return ResponseEntity.ok(
                ApiResponse.success("Applications retrieved successfully.", projectService.getApplications()).getData()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createApplication(
             @RequestBody CreateProjectRequest request
    ) {

        ProjectResponse response =
                projectService.createApplication(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created successfully.",response));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateApplication(
            @PathVariable UUID id,
             @RequestBody UpdateProjectRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application updated successfully.",
                        projectService.update(id,request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(
            @PathVariable UUID id
    ) {
    projectService.deleteApplicationById(id);
    return ResponseEntity.ok(ApiResponse.success("Application deleted successfully.", null));
    }

}
