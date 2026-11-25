package com.silentvoix.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.database.DirectTransactions;
import com.silentvoix.backend.database.DuplicateKeyExceptions;
import org.junit.jupiter.api.Test;

/** The sign-up and sign-in rules against in-memory repositories; storage is covered by AuthApiTest. */
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeSessions sessions = new FakeSessions();
    private final PasswordHasher hasher = new PasswordHasher();
    private final AuthService auth = new AuthService(new DirectTransactions(), accounts, sessions, hasher,
            new LoginThrottle(Clock.fixed(NOW, ZoneOffset.UTC)), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void registerMakesAnOrdinaryUserWithAThirtyDaySession() {
        AuthService.Session session = auth.register(" Lan@Example.com ", "long-enough", "Lan", null, null);

        assertEquals("lan@example.com", session.user().email());
        assertEquals("user", session.user().role());
        assertEquals(NOW.plus(AuthService.SESSION_LIFETIME), session.expiresAt());
        assertNotNull(auth.authenticate(session.token()));
    }

    @Test
    void aTakenAddressIsAConflict() {
        auth.register("lan@example.com", "long-enough", null, null, null);

        ApiException e = assertThrows(ApiException.class,
                () -> auth.register("lan@example.com", "other-password", null, null, null));
        assertEquals("email_taken", e.code());
    }

    @Test
    void unknownAddressAndWrongPasswordGetTheSameAnswer() {
        auth.register("lan@example.com", "long-enough", null, null, null);

        assertEquals("invalid_credentials", assertThrows(ApiException.class,
                () -> auth.login("nobody@example.com", "long-enough", null, null)).code());
        assertEquals("invalid_credentials", assertThrows(ApiException.class,
                () -> auth.login("lan@example.com", "wrong-password", null, null)).code());
    }

    @Test
    void aLockedAccountIsNamedOnlyWithTheRightPassword() {
        auth.register("lan@example.com", "long-enough", null, null, null);
        accounts.locked.add("lan@example.com");

        assertEquals("invalid_credentials", assertThrows(ApiException.class,
                () -> auth.login("lan@example.com", "wrong-password", null, null)).code());
        assertEquals("account_disabled", assertThrows(ApiException.class,
                () -> auth.login("lan@example.com", "long-enough", null, null)).code());
    }

    @Test
    void logoutEndsTheSession() {
        AuthService.Session session = auth.register("lan@example.com", "long-enough", null, null, null);
        CurrentUser user = auth.authenticate(session.token());

        auth.logout(user);

        assertNull(auth.authenticate(session.token()));
        assertTrue(sessions.revoked.contains(user.sessionId()));
    }

    private static final class FakeAccounts implements AccountRepository {
        final Map<UUID, String> names = new HashMap<>();
        final Map<String, UUID> emails = new HashMap<>();
        final Map<UUID, String> hashes = new HashMap<>();
        final List<String> locked = new ArrayList<>();

        @Override
        public UUID createUser(String displayName) {
            UUID id = UUID.randomUUID();
            names.put(id, displayName);
            return id;
        }

        @Override
        public void addEmailIdentity(UUID userId, String email) {
            if (emails.putIfAbsent(email, userId) != null) {
                throw DuplicateKeyExceptions.of(new SQLException("duplicate", "23505"));
            }
        }

        @Override
        public void addPasswordHash(UUID userId, String passwordHash) {
            hashes.put(userId, passwordHash);
        }

        @Override
        public Optional<LoginRecord> findByEmail(String email) {
            return Optional.ofNullable(emails.get(email)).map(id -> new LoginRecord(
                    new Account(id, email, names.get(id), "user"), locked.contains(email), hashes.get(id)));
        }
    }

    private static final class FakeSessions implements SessionRepository {
        private record Stored(byte[] hash, UUID deviceId, UUID sessionId) {
        }

        final Map<UUID, UUID> deviceOwners = new HashMap<>();
        final List<Stored> stored = new ArrayList<>();
        final List<UUID> revoked = new ArrayList<>();

        @Override
        public UUID addDevice(UUID userId, String platform, String appVersion) {
            UUID id = UUID.randomUUID();
            deviceOwners.put(id, userId);
            return id;
        }

        @Override
        public void addSession(UUID deviceId, byte[] tokenHash, Instant expiresAt) {
            stored.add(new Stored(tokenHash, deviceId, UUID.randomUUID()));
        }

        @Override
        public Optional<CurrentUser> findActive(byte[] tokenHash) {
            return stored.stream()
                    .filter(s -> Arrays.equals(s.hash(), tokenHash) && !revoked.contains(s.sessionId()))
                    .findFirst()
                    .map(s -> new CurrentUser(new Account(deviceOwners.get(s.deviceId()), null, null, "user"),
                            s.deviceId(), s.sessionId()));
        }

        @Override
        public void touchDevice(UUID deviceId) {
        }

        @Override
        public void revoke(UUID sessionId) {
            revoked.add(sessionId);
        }

        @Override
        public void revokeAllFor(UUID userId) {
        }
    }
}
