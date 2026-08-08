package org.example.Deployment.Repository;

import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentJobRepository extends JpaRepository<DeploymentJob, UUID> {
    List<DeploymentJob> findByStatus(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j from DeploymentJob j
            where j.status = org.example.Deployment.Enums.JobStatus.QUEUED
              and (j.nextAttemptAt is null or j.nextAttemptAt <= :now)
              and not exists (
                select active.id from DeploymentJob active
                where active.deploymentId = j.deploymentId
                  and active.status in (org.example.Deployment.Enums.JobStatus.APPLYING,
                                        org.example.Deployment.Enums.JobStatus.ROLLING_OUT)
              )
            order by j.createdAt asc
            """)
    List<DeploymentJob> findClaimable(@Param("now") LocalDateTime now, Pageable pageable);

    List<DeploymentJob> findByStatusAndStartedAtBefore(JobStatus status, LocalDateTime threshold);
}
