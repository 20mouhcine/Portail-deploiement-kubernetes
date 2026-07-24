package org.example.Deployment.Repository;

import org.example.Deployment.Entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {

    List<Deployment> findAllByOrderByCreatedAtDesc();

    boolean existsByNameIgnoreCaseAndNamespaceIgnoreCase(String name, String namespace);

    boolean existsByNameIgnoreCaseAndNamespaceIgnoreCaseAndIdNot(
            String name,
            String namespace,
            UUID id
    );
}
