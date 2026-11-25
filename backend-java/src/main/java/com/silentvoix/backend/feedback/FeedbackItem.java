package com.silentvoix.backend.feedback;

import java.time.Instant;
import java.util.UUID;

/** A piece of feedback as the admin inbox shows it. The author fields are null for a deleted account. */
public record FeedbackItem(UUID id, String kind, String message, Instant createdAt, Instant resolvedAt,
                           String authorEmail, String authorName) {
}
