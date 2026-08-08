package org.example.auth.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        boolean retryable,
        Map<String, String> fieldErrors
) {
}
