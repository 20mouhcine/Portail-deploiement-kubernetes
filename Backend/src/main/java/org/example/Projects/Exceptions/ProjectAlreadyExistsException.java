package org.example.Projects.Exceptions;

/**
 * @author pc
 **/
public class ProjectAlreadyExistsException extends RuntimeException {
    public ProjectAlreadyExistsException(String message) {
        super(message);
    }
}
