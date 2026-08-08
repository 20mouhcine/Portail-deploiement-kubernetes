package org.example.Projects.Repository;

import org.example.Projects.Entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author pc
 **/
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Project findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    @EntityGraph(attributePaths = {"owner", "allowedUsers", "allowedNamespaces"})
    java.util.List<Project> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"owner", "allowedUsers", "allowedNamespaces"})
    java.util.Optional<Project> findWithAccessById(UUID id);
}
