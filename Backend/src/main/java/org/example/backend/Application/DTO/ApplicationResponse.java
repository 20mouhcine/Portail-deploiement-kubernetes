package org.example.backend.Application.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.Application.Entity.Application;

import java.util.UUID;

/**
 * @author pc
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationResponse {

    private UUID id;

    private String name;

    private String description;

    private String repository;

    public static ApplicationResponse from(Application app) {
        return new ApplicationResponse(
                app.getId(),
                app.getName(),
                app.getDescription(),
                app.getRepository()
        );
    }
}
