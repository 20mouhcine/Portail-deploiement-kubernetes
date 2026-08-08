package org.example.Projects.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.auth.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_allowed_users",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_project_allowed_user",
                    columnNames = {"project_id", "user_id"}
            )
    )
    Set<User> allowedUsers = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_allowed_namespaces", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "namespace", nullable = false, length = 63)
    Set<String> allowedNamespaces = new HashSet<>();

    @Column(length = 30)
    String environmentType;

    @Column(length = 30)
    String deploymentPolicy;

    @Column(length = 20)
    String cpuQuota;

    @Column(length = 20)
    String memoryQuota;

    Integer podQuota;

}
