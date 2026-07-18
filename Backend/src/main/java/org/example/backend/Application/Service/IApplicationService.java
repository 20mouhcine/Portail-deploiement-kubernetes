package org.example.backend.Application.Service;

import org.example.backend.Application.DTO.ApplicationResponse;
import org.example.backend.Application.DTO.CreateApplicationRequest;
import org.example.backend.Application.DTO.UpdateApplicationRequest;
import org.example.backend.Application.Entity.Application;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@Service
public interface IApplicationService {
    List<ApplicationResponse> getApplications();
    Application getApplicationByName(String name);
    Application getApplicationById(UUID id);
    ApplicationResponse createApplication(CreateApplicationRequest request);

    void deleteApplicationById(UUID id);

    ApplicationResponse update(UUID id, UpdateApplicationRequest request);
}
