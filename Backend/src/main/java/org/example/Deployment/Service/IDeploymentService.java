package org.example.Deployment.Service;

import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentDetailResponse;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.PodResponse;
import org.example.Deployment.DTO.UpdateDeploymentRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface IDeploymentService {

    List<DeploymentResponse> findAll();

    DeploymentResponse findById(UUID id);

    DeploymentDetailResponse getDetail(UUID id);

    DeploymentResponse create(CreateDeploymentRequest request, String username);

    DeploymentResponse update(UUID id, UpdateDeploymentRequest request);

    DeploymentResponse restart(UUID id);

    DeploymentResponse stop(UUID id);

    DeploymentResponse scale(UUID id, Integer replicas);

    DeploymentResponse rollback(UUID id);

    void delete(UUID id);

    @Transactional(readOnly = true)
    String getLogs(UUID id);

    List<PodResponse> getPods(UUID id);
}
