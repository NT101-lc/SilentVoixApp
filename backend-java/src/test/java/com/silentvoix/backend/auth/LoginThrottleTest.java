package com.silentvoix.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    private Instant now = Instant.parse("2026-10-03T08:00:00Z");

    private final LoginThrottle throttle = new LoginThrottle(new Clock() {
        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    });

    @Test
    void fiveFailuresAreAllowedTheSixthAttemptIsNot() {
        for (int i = 0; i < 5; i++) {
            assertThat(throttle.isBlocked("lan@example.com")).isFalse();
            throttle.recordFailure("lan@example.com");
        }

        assertThat(throttle.isBlocked("lan@example.com")).isTrue();
    }

    @Test
    void otherAddressesAreNotAffected() {
        for (int i = 0; i < 5; i++) {
            throttle.recordFailure("lan@example.com");
        }

        assertThat(throttle.isBlocked("minh@example.com")).isFalse();
    }

    @Test
    void theBlockLiftsFifteenMinutesAfterTheFirstFailure() {
        for (int i = 0; i < 5; i++) {
            throttle.recordFailure("lan@example.com");
        }

        now = now.plus(Duration.ofMinutes(15));

        assertThat(throttle.isBlocked("lan@example.com")).isFalse();
    }

    @Test
    void aSuccessfulSignInClearsTheCount() {
        for (int i = 0; i < 4; i++) {
            throttle.recordFailure("lan@example.com");
        }

        throttle.recordSuccess("lan@example.com");
        throttle.recordFailure("lan@example.com");

        assertThat(throttle.isBlocked("lan@example.com")).isFalse();
    }
}
