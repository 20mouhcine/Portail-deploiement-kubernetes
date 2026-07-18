package org.example.backend.Application.Exceptions;

import org.example.backend.Application.DTO.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author pc
 **/
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleAlreadyExists(
            ApplicationAlreadyExistsException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }
}