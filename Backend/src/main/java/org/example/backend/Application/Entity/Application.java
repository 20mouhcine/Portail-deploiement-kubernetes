package org.example.backend.Application.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author pc
 **/


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)

    UUID id;

    @Column(unique = true)
    String name;

    String description;

    String repository;

    LocalDateTime createdAt;

}
