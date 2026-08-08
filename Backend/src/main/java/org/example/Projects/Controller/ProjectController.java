package org.example.Projects.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.History.Enums.ActionType;
import org.example.History.Enums.TargetType;
import org.example.History.Service.IActionHistoryService;
import org.example.Projects.DTO.ApiResponse;
import org.example.Projects.DTO.ProjectResponse;
import org.example.Projects.DTO.CreateProjectRequest;
import org.example.Projects.DTO.UpdateProjectRequest;
import org.example.Projects.Entity.Project;
import org.example.Projects.Service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final IActionHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getApplications(Authentication authentication) {
        return ResponseEntity.ok(
                ApiResponse.success("Applications retrieved successfully.", projectService.getApplications(authentication)).getData()
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createApplication(
             @Valid @RequestBody CreateProjectRequest request,
             Authentication authentication,
             HttpServletRequest httpRequest
    ) {

        ProjectResponse response =
                projectService.createApplication(request, authentication);
        record(ActionType.CREATE, "Création du projet", response.getName(), authentication, httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created successfully.",response));
    }
    @PutMapping("/{id}")
    @PreAuthorize("@projectAccess.canManage(#id, authentication)")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateApplication(
            @PathVariable UUID id,
             @Valid @RequestBody UpdateProjectRequest request,
             Authentication authentication,
             HttpServletRequest httpRequest
    ) {

        ProjectResponse response = projectService.update(id,request);
        record(ActionType.UPDATE, "Modification du projet", response.getName(), authentication, httpRequest);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application updated successfully.",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectAccess.canManage(#id, authentication)")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        Project project = projectService.getApplicationById(id);
        String projectName = project == null ? id.toString() : project.getName();
        projectService.deleteApplicationById(id);
        record(ActionType.DELETE, "Suppression du projet", projectName, authentication, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Application deleted successfully.", null));
    }

    private void record(
            ActionType action,
            String details,
            String projectName,
            Authentication authentication,
            HttpServletRequest request
    ) {
        historyService.record(
                action,
                details,
                TargetType.PROJECT,
                projectName,
                authentication.getName(),
                request.getRemoteAddr()
        );
    }

}
