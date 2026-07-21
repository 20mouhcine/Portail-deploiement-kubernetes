package org.example.Projects.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author pc
 **/

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateProjectRequest {
    private UUID id;

    private String name;

    private String description;

    private String repository;

    private UUID owner_id;

}
