package org.example.backend.Application.Exceptions;

/**
 * @author pc
 **/
public class ApplicationAlreadyExistsException extends RuntimeException {
    public ApplicationAlreadyExistsException(String message) {
        super(message);
    }
}
