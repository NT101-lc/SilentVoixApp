package com.silentvoix.backend.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Slows password guessing: after {@value #MAX_FAILURES} wrong passwords for one address within
 * {@link #WINDOW}, that address is refused until the window, counted from the first failure, ends.
 * Kept in memory, which is enough for one backend instance.
 */
@Component
class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record Failures(Instant firstAt, int count) {
    }

    private final Clock clock;
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    @Autowired
    LoginThrottle() {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    boolean isBlocked(String email) {
        Failures current = failures.get(email);
        if (current == null) {
            return false;
        }
        if (expired(current)) {
            failures.remove(email, current);
            return false;
        }
        return current.count() >= MAX_FAILURES;
    }

    void recordFailure(String email) {
        failures.values().removeIf(this::expired);
        failures.merge(email, new Failures(clock.instant(), 1),
                (old, fresh) -> expired(old) ? fresh : new Failures(old.firstAt(), old.count() + 1));
    }

    void recordSuccess(String email) {
        failures.remove(email);
    }

    private boolean expired(Failures entry) {
        return !clock.instant().isBefore(entry.firstAt().plus(WINDOW));
    }
}
