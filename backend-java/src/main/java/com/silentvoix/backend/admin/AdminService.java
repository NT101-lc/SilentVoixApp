package com.silentvoix.backend.admin;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.auth.CurrentUser;
import com.silentvoix.backend.auth.SessionRepository;
import com.silentvoix.backend.database.Transactions;
import com.silentvoix.backend.feedback.FeedbackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** The admin overview and account management (role, lock). Callers must already be admins. */
@Service
class AdminService {

    private static final Set<String> ROLES = Set.of(CurrentUser.ROLE_USER, CurrentUser.ROLE_ADMIN);
    private static final int LIST_LIMIT = 500;

    private final Transactions transactions;
    private final AdminUserRepository users;
    private final SessionRepository sessions;
    private final FeedbackRepository feedback;

    AdminService(Transactions transactions, AdminUserRepository users, SessionRepository sessions,
                 FeedbackRepository feedback) {
        this.transactions = transactions;
        this.users = users;
        this.sessions = sessions;
        this.feedback = feedback;
    }

    Overview overview() {
        return transactions.inTransaction(() -> {
            AdminUserRepository.Counts accounts = users.counts();
            FeedbackRepository.Counts inbox = feedback.counts();
            return new Overview(accounts.users(), accounts.admins(), accounts.locked(), accounts.newThisWeek(),
                    accounts.activeThisWeek(), inbox.open(), inbox.total());
        });
    }

    List<AdminUser> users() {
        return transactions.inTransaction(() -> users.list(LIST_LIMIT));
    }

    /**
     * Changes [target]'s role and/or lock (null leaves it as is). An admin cannot change their own
     * account, which keeps the last admin from locking or demoting themselves out of the app.
     * Locking signs the account out everywhere at once, not when its sessions expire.
     */
    AdminUser changeUser(CurrentUser admin, UUID target, String role, Boolean locked) {
        if (role != null && !ROLES.contains(role)) {
            throw ApiException.badRequest("invalid_role");
        }
        if (target.equals(admin.id())) {
            throw new ApiException(HttpStatus.CONFLICT, "cannot_change_self");
        }
        return transactions.inTransaction(() -> {
            if (users.find(target).isEmpty()) {
                throw ApiException.notFound();
            }
            if (role != null) {
                users.setRole(target, role);
            }
            if (Boolean.TRUE.equals(locked)) {
                users.lock(target);
                sessions.revokeAllFor(target);
            } else if (Boolean.FALSE.equals(locked)) {
                users.unlock(target);
            }
            return users.find(target).orElseThrow(ApiException::notFound);
        });
    }
}
