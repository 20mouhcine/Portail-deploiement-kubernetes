package org.example.Deployment.Service;

import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.UpdateDeploymentRequest;

import java.util.List;
import java.util.UUID;

public interface IDeploymentService {

    List<DeploymentResponse> findAll();

    DeploymentResponse findById(UUID id);

    DeploymentResponse create(CreateDeploymentRequest request, String username);

    DeploymentResponse update(UUID id, UpdateDeploymentRequest request);

    DeploymentResponse restart(UUID id);

    DeploymentResponse stop(UUID id);

    DeploymentResponse scale(UUID id, Integer replicas);

    void delete(UUID id);
}
