package org.example.Deployment.Repository;

import org.example.Deployment.Entity.DeploymentRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentRevisionRepository extends JpaRepository<DeploymentRevision, UUID> {

    List<DeploymentRevision> findByDeploymentIdOrderByRevisionNumberDesc(UUID deploymentId);

    long countByDeploymentId(UUID deploymentId);
}