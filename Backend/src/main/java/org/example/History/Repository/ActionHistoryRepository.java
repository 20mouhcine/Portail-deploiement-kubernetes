package org.example.History.Repository;

import org.example.History.Entity.ActionHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionHistoryRepository extends JpaRepository<ActionHistory, UUID> {

    @EntityGraph(attributePaths = "user")
    List<ActionHistory> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<ActionHistory> findByUserUsernameIgnoreCaseOrderByCreatedAtDesc(String username);
}
