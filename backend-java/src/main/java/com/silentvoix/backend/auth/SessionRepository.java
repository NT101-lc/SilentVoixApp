package com.silentvoix.backend.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Devices and their sessions ({@code device}, {@code refresh_token}). Only token hashes are
 * stored. Called inside a service transaction.
 */
public interface SessionRepository {

    /** A device row for one sign-in; returns its id. */
    UUID addDevice(UUID userId, String platform, String appVersion);

    void addSession(UUID deviceId, byte[] tokenHash, Instant expiresAt);

    /** The caller behind a live (not revoked, not expired) session of an unlocked account. */
    Optional<CurrentUser> findActive(byte[] tokenHash);

    /** Marks the device as seen now, at most once a minute. */
    void touchDevice(UUID deviceId);

    void revoke(UUID sessionId);

    /** Signs [userId] out on every device. */
    void revokeAllFor(UUID userId);
}
