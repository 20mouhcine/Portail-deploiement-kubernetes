package org.example.backend.Application.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author pc
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    private String name;

    private String description;

    private String repository;

}