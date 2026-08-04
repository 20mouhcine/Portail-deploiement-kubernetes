package org.example.Deployment.Repository;

import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentJobRepository extends JpaRepository<DeploymentJob, UUID> {
    List<DeploymentJob> findByStatus(JobStatus status);
}
