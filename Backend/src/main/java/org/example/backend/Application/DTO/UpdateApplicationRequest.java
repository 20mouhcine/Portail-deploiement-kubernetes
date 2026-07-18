package org.example.backend.Application.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author pc
 **/

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateApplicationRequest {

    private String name;

    private String description;

    private String repository;

}
