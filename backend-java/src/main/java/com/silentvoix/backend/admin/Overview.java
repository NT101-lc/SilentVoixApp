package com.silentvoix.backend.admin;

/** Body of {@code GET /api/v1/admin/overview}. "This week" is the last seven days. */
public record Overview(long users, long admins, long lockedUsers, long newUsersThisWeek, long activeUsersThisWeek,
                       long openFeedback, long totalFeedback) {
}
