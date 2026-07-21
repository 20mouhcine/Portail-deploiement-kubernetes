package org.example.Projects.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.auth.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author pc
 **/


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)

    UUID id;

    @Column(unique = true)
    String name;

    String description;

    String repository;

    LocalDateTime createdAt;
    @ManyToOne
    User owner;

}
