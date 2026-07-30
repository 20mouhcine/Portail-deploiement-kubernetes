package org.example.Deployment.Repository;

import org.example.Deployment.Entity.DeploymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentEventRepository extends JpaRepository<DeploymentEvent, UUID> {
    List<DeploymentEvent> findByDeploymentIdOrderByTimestampAsc(UUID deploymentId);

    List<DeploymentEvent> findByDeploymentIdOrderByTimestampDesc(UUID deploymentId);
}
