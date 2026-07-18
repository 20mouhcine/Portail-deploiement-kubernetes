package org.example.backend.Application.Repository;

import org.example.backend.Application.Entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author pc
 **/
@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Application findByName(String name);

    boolean existsByName(String repository);
}
