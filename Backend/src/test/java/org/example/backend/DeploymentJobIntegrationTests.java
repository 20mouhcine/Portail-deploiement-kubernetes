package org.example.backend;

import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.JobOperationType;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.ServiceImpl.DeploymentJobClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DeploymentJobIntegrationTests {
    @Autowired DeploymentJobRepository repository;
    @Autowired DeploymentJobClaimService claimService;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void onlyOneJobPerDeploymentCanBeClaimedAtATime() {
        UUID deploymentId = UUID.randomUUID();
        repository.save(job(deploymentId));
        repository.save(job(deploymentId));

        UUID claimed = claimService.claimNext().orElseThrow();

        assertThat(repository.findById(claimed).orElseThrow().getStatus()).isEqualTo(JobStatus.APPLYING);
        assertThat(claimService.claimNext()).isEmpty();
    }

    private DeploymentJob job(UUID deploymentId) {
        return DeploymentJob.builder()
                .deploymentId(deploymentId)
                .operationType(JobOperationType.UPDATE)
                .status(JobStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }
}
