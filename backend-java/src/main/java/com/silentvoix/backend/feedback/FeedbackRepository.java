package com.silentvoix.backend.feedback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stored feedback. Called inside a service transaction. */
public interface FeedbackRepository {

    record Counts(long open, long total) {
    }

    /** Returns the new item's id. */
    UUID create(UUID userId, UUID deviceId, String kind, String message);

    /** Newest first, at most [limit]; [openOnly] leaves out resolved items. */
    List<FeedbackItem> list(boolean openOnly, int limit);

    Optional<FeedbackItem> find(UUID id);

    /** Resolving keeps the first resolution time; reopening clears it. Unknown ids are ignored. */
    void setResolved(UUID id, boolean resolved);

    Counts counts();
}
