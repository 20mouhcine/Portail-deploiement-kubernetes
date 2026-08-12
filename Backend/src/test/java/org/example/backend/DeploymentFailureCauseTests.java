package org.example.backend;

import org.example.Deployment.DTO.DeploymentDetailResponse;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Repository.DeploymentRevisionRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.example.Deployment.ServiceImpl.DeploymentServiceImpl;
import org.example.Deployment.ServiceImpl.DeploymentStatusSynchronizer;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.example.Projects.Security.ProjectAccessService;
import org.example.auth.entity.User;
import org.example.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentFailureCauseTests {

    @Mock DeploymentRepository deploymentRepository;
    @Mock DeploymentRevisionRepository deploymentRevisionRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock IKubernetesDeploymentService kubernetesDeploymentService;
    @Mock DeploymentStatusSynchronizer deploymentStatusSynchronizer;
    @Mock IDeploymentEventService deploymentEventService;
    @Mock DeploymentJobRepository deploymentJobRepository;
    @Mock ProjectAccessService projectAccessService;

    @Test
    void exposesTheLatestFailedJobMessageWhenKubernetesHasNoReason() {
        UUID deploymentId = UUID.randomUUID();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Portal");
        User user = new User("devops", "devops@example.com", "hash");
        Deployment deployment = Deployment.create();
        deployment.setProject(project);
        deployment.setDeployedBy(user);
        deployment.setName("backend-api");
        deployment.setNamespace("production");
        deployment.setReplicas(1);
        deployment.setImage("registry.example.com/backend:1.0");
        deployment.setPort(8080);
        deployment.setCpu("250m");
        deployment.setMemory("256Mi");
        deployment.setStatus(DeploymentStatus.FAILED);
        DeploymentJob failedJob = DeploymentJob.builder()
                .errorMessage("Registry not allowed: registry.example.com")
                .status(JobStatus.FAILED)
                .build();

        when(deploymentRepository.findById(deploymentId)).thenReturn(Optional.of(deployment));
        when(kubernetesDeploymentService.getPods(deployment)).thenReturn(List.of());
        when(deploymentRevisionRepository.findByDeploymentIdOrderByRevisionNumberDesc(deploymentId))
                .thenReturn(List.of());
        when(deploymentEventService.findByDeploymentIdOrderByTimestampDesc(deploymentId))
                .thenReturn(List.of());
        when(deploymentJobRepository.findFirstByDeploymentIdAndStatusOrderByCreatedAtDesc(
                deploymentId, JobStatus.FAILED)).thenReturn(Optional.of(failedJob));

        DeploymentServiceImpl service = new DeploymentServiceImpl(
                deploymentRepository,
                deploymentRevisionRepository,
                projectRepository,
                userRepository,
                kubernetesDeploymentService,
                deploymentStatusSynchronizer,
                deploymentEventService,
                deploymentJobRepository,
                projectAccessService);

        DeploymentDetailResponse response = service.getDetail(deploymentId);

        assertThat(response.getFailureCause())
                .isEqualTo("Registry not allowed: registry.example.com");
    }
}
