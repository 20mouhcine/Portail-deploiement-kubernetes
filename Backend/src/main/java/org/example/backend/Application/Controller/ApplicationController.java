package org.example.backend.Application.Controller;

import lombok.AllArgsConstructor;
import org.example.backend.Application.DTO.ApiResponse;
import org.example.backend.Application.DTO.ApplicationResponse;
import org.example.backend.Application.DTO.CreateApplicationRequest;
import org.example.backend.Application.DTO.UpdateApplicationRequest;
import org.example.backend.Application.Service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@RestController
@RequestMapping("/api/apps")
@AllArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getApplications() {
        return ResponseEntity.ok(
                ApiResponse.success("Applications retrieved successfully.", applicationService.getApplications()).getData()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
             @RequestBody CreateApplicationRequest request
    ) {

        ApplicationResponse response =
                applicationService.createApplication(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created successfully.",response));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplication(
            @PathVariable UUID id,
             @RequestBody UpdateApplicationRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application updated successfully.",
                        applicationService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(
            @PathVariable UUID id
    ) {
    applicationService.deleteApplicationById(id);
    return ResponseEntity.ok(ApiResponse.success("Application deleted successfully.", null));
    }

}
