package com.silentvoix.backend.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accounts as admins manage them. Called inside a service transaction. */
public interface AdminUserRepository {

    record Counts(long users, long admins, long locked, long newThisWeek, long activeThisWeek) {
    }

    /** Newest first, at most [limit]. */
    List<AdminUser> list(int limit);

    Optional<AdminUser> find(UUID id);

    void setRole(UUID id, String role);

    /** Keeps the first lock time if already locked. */
    void lock(UUID id);

    void unlock(UUID id);

    Counts counts();
}
