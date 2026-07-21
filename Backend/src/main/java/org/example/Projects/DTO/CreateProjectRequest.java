package org.example.Projects.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author pc
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    private String name;

    private String description;

    private String repository;

    private UUID owner_id;

}