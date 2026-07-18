package org.example.backend.serviceimpl;

import org.example.backend.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final String UNKNOWN_VALUE = "<unknown>";

    private final int accountMaxAttempts;
    private final int ipMaxAttempts;
    private final Duration attemptWindow;
    private final Duration blockDuration;
    private final int maxTrackedKeys;
    private final Map<String, AttemptState> attempts =
            new LinkedHashMap<>(128, 0.75f, true);

    public LoginAttemptServiceImpl(
            @Value("${app.security.login.max-attempts:5}") int accountMaxAttempts,
            @Value("${app.security.login.ip-max-attempts:20}") int ipMaxAttempts,
            @Value("${app.security.login.attempt-window:15m}") Duration attemptWindow,
            @Value("${app.security.login.block-duration:15m}") Duration blockDuration,
            @Value("${app.security.login.max-tracked-keys:10000}") int maxTrackedKeys) {
        this.accountMaxAttempts = Math.max(1, accountMaxAttempts);
        this.ipMaxAttempts = Math.max(this.accountMaxAttempts, ipMaxAttempts);
        this.attemptWindow = attemptWindow;
        this.blockDuration = blockDuration;
        this.maxTrackedKeys = Math.max(100, maxTrackedKeys);
    }

    @Override
    public synchronized long retryAfterSeconds(String username, String clientAddress) {
        Instant now = Instant.now();
        pruneExpired(now);

        long accountRetry = retryAfter(accountKey(username), now);
        long ipRetry = retryAfter(ipKey(clientAddress), now);
        return Math.max(accountRetry, ipRetry);
    }

    @Override
    public synchronized void recordFailure(String username, String clientAddress) {
        Instant now = Instant.now();
        pruneExpired(now);
        update(accountKey(username), accountMaxAttempts, now);
        update(ipKey(clientAddress), ipMaxAttempts, now);
    }

    @Override
    public synchronized void recordSuccess(String username, String clientAddress) {
        attempts.remove(accountKey(username));
        attempts.remove(ipKey(clientAddress));
    }

    private void update(String key, int limit, Instant now) {
        AttemptState state = attempts.get(key);
        if (state == null || !now.isBefore(state.windowEndsAt())) {
            ensureCapacity(now);
            state = new AttemptState(0, now.plus(attemptWindow), null);
        }

        int failures = state.failures() + 1;
        Instant blockedUntil = failures >= limit ? now.plus(blockDuration) : null;
        attempts.put(key, new AttemptState(failures, state.windowEndsAt(), blockedUntil));
    }

    private long retryAfter(String key, Instant now) {
        AttemptState state = attempts.get(key);
        if (state == null || state.blockedUntil() == null || !now.isBefore(state.blockedUntil())) {
            return 0;
        }

        return Math.max(1, Duration.between(now, state.blockedUntil()).toSeconds());
    }

    private void ensureCapacity(Instant now) {
        pruneExpired(now);
        if (attempts.size() < maxTrackedKeys) {
            return;
        }

        Iterator<String> iterator = attempts.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private void pruneExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            Instant expiresAt = state.blockedUntil() == null
                    ? state.windowEndsAt()
                    : state.blockedUntil();
            return !now.isBefore(expiresAt);
        });
    }

    private String accountKey(String username) {
        String normalized = username == null || username.isBlank()
                ? UNKNOWN_VALUE
                : username.trim().toLowerCase(Locale.ROOT);
        return "account:" + normalized;
    }

    private String ipKey(String clientAddress) {
        String normalized = clientAddress == null || clientAddress.isBlank()
                ? UNKNOWN_VALUE
                : clientAddress.trim();
        return "ip:" + normalized;
    }

    private record AttemptState(
            int failures,
            Instant windowEndsAt,
            Instant blockedUntil) {
    }
}
