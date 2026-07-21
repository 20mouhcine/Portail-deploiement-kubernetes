package org.example.auth.dto;

public record CsrfResponse(
        String token,
        String headerName,
        String parameterName
) {
}
