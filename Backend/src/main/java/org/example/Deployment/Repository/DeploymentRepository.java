package org.example.Deployment.Repository;

import org.example.Deployment.Entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

    Optional<Deployment> findByNameIgnoreCaseAndNamespaceIgnoreCase(String name, String namespace);

    @EntityGraph(attributePaths = {"project", "project.owner", "project.allowedUsers", "project.allowedNamespaces"})
    Optional<Deployment> findWithProjectAccessById(UUID id);

    @EntityGraph(attributePaths = {"project", "project.owner", "project.allowedUsers", "project.allowedNamespaces"})
    List<Deployment> findAllWithProjectByOrderByCreatedAtDesc();
}
