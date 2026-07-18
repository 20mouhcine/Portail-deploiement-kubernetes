package org.example.backend.service;

public interface LoginAttemptService {

    long retryAfterSeconds(String username, String clientAddress);

    void recordFailure(String username, String clientAddress);

    void recordSuccess(String username, String clientAddress);
}
