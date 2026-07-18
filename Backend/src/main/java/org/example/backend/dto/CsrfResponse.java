package org.example.backend.dto;

public record CsrfResponse(
        String token,
        String headerName,
        String parameterName
) {
}
