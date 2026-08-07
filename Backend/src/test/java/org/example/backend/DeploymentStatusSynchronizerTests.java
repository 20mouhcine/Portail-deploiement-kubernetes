package org.example.backend;

import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.example.Deployment.ServiceImpl.DeploymentStatusSynchronizer;
import org.example.Projects.Entity.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentStatusSynchronizerTests {

    @Mock
    DeploymentRepository deploymentRepository;

    @Mock
    IDeploymentEventService deploymentEventService;

    @Test
    void marksTheDeploymentAsFailedAndPublishesTheStatusChange() {
        UUID deploymentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        Deployment deployment = Deployment.create();
        deployment.setProject(project);
        deployment.setStatus(DeploymentStatus.RUNNING);
        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        DeploymentStatusSynchronizer synchronizer =
                new DeploymentStatusSynchronizer(deploymentRepository, deploymentEventService);

        synchronizer.markFailed(deploymentId);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.FAILED);
        verify(deploymentRepository).save(deployment);
        verify(deploymentEventService)
                .publishDeploymentStatusChange(projectId, deploymentId, DeploymentStatus.FAILED.name());
    }

    @Test
    void doesNotPublishTheSameFailureStatusTwice() {
        UUID deploymentId = UUID.randomUUID();
        Deployment deployment = Deployment.create();
        deployment.setStatus(DeploymentStatus.FAILED);
        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        DeploymentStatusSynchronizer synchronizer =
                new DeploymentStatusSynchronizer(deploymentRepository, deploymentEventService);

        synchronizer.markFailed(deploymentId);

        verify(deploymentRepository, never()).save(deployment);
        verify(deploymentEventService, never())
                .publishDeploymentStatusChange(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
