package org.example.Projects.Repository;

import org.example.Projects.Entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author pc
 **/
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Project findByName(String name);

    boolean existsByName(String repository);
}
