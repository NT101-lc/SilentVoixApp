package com.silentvoix.backend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.auth.Account;
import com.silentvoix.backend.auth.CurrentUser;
import com.silentvoix.backend.auth.SessionRepository;
import com.silentvoix.backend.database.DirectTransactions;
import com.silentvoix.backend.feedback.FeedbackItem;
import com.silentvoix.backend.feedback.FeedbackRepository;
import org.junit.jupiter.api.Test;

/** Account-management rules against in-memory repositories; storage is covered by AdminApiTest. */
class AdminServiceTest {

    private final FakeUsers users = new FakeUsers();
    private final List<UUID> signedOut = new ArrayList<>();
    private final DirectTransactions transactions = new DirectTransactions();
    private final AdminService admin = new AdminService(transactions, users, new RevokeOnlySessions(signedOut),
            new CountOnlyFeedback());

    private final UUID adminId = users.add("admin");
    private final CurrentUser caller = new CurrentUser(new Account(adminId, "a@x.vn", null, "admin"),
            UUID.randomUUID(), UUID.randomUUID());

    @Test
    void lockingSignsTheAccountOutEverywhere() {
        UUID target = users.add("user");

        AdminUser changed = admin.changeUser(caller, target, null, true);

        assertTrue(changed.locked());
        assertEquals(List.of(target), signedOut);
    }

    @Test
    void anAdminCannotChangeThemselves() {
        ApiException e = assertThrows(ApiException.class, () -> admin.changeUser(caller, adminId, "user", null));
        assertEquals("cannot_change_self", e.code());
    }

    @Test
    void anUnknownRoleIsRefusedBeforeTouchingTheDatabase() {
        ApiException e = assertThrows(ApiException.class,
                () -> admin.changeUser(caller, users.add("user"), "owner", null));
        assertEquals("invalid_role", e.code());
        assertEquals(0, transactions.opened);
    }

    @Test
    void anUnknownAccountIsNotFound() {
        ApiException e = assertThrows(ApiException.class,
                () -> admin.changeUser(caller, UUID.randomUUID(), "admin", null));
        assertEquals("not_found", e.code());
    }

    @Test
    void theOverviewCombinesAccountsAndFeedbackInOneTransaction() {
        Overview overview = admin.overview();

        assertEquals(1, overview.users());
        assertEquals(3, overview.openFeedback());
        assertEquals(1, transactions.opened);
    }

    private static final class FakeUsers implements AdminUserRepository {
        final Map<UUID, AdminUser> byId = new LinkedHashMap<>();

        UUID add(String role) {
            UUID id = UUID.randomUUID();
            byId.put(id, new AdminUser(id, id + "@x.vn", null, role, false, Instant.EPOCH, null));
            return id;
        }

        private void change(UUID id, String role, boolean locked) {
            AdminUser u = byId.get(id);
            byId.put(id, new AdminUser(id, u.email(), u.displayName(), role, locked, u.createdAt(), u.lastSeenAt()));
        }

        @Override
        public List<AdminUser> list(int limit) {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<AdminUser> find(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void setRole(UUID id, String role) {
            change(id, role, byId.get(id).locked());
        }

        @Override
        public void lock(UUID id) {
            change(id, byId.get(id).role(), true);
        }

        @Override
        public void unlock(UUID id) {
            change(id, byId.get(id).role(), false);
        }

        @Override
        public Counts counts() {
            return new Counts(byId.size(), 1, 0, 0, 0);
        }
    }

    private record RevokeOnlySessions(List<UUID> signedOut) implements SessionRepository {
        @Override
        public UUID addDevice(UUID userId, String platform, String appVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addSession(UUID deviceId, byte[] tokenHash, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CurrentUser> findActive(byte[] tokenHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void touchDevice(UUID deviceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revoke(UUID sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revokeAllFor(UUID userId) {
            signedOut.add(userId);
        }
    }

    private static final class CountOnlyFeedback implements FeedbackRepository {
        @Override
        public UUID create(UUID userId, UUID deviceId, String kind, String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<FeedbackItem> list(boolean openOnly, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FeedbackItem> find(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResolved(UUID id, boolean resolved) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Counts counts() {
            return new Counts(3, 5);
        }
    }
}
