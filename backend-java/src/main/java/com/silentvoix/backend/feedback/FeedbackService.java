package com.silentvoix.backend.feedback;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.auth.CurrentUser;
import com.silentvoix.backend.database.Transactions;
import org.springframework.stereotype.Service;

/** Feedback from any signed-in user, and the admin inbox that handles it. */
@Service
public class FeedbackService {

    static final Set<String> KINDS = Set.of("wrong_result", "bug", "idea");
    static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int LIST_LIMIT = 500;

    private final Transactions transactions;
    private final FeedbackRepository feedback;

    FeedbackService(Transactions transactions, FeedbackRepository feedback) {
        this.transactions = transactions;
        this.feedback = feedback;
    }

    /** Stores a wrong result, bug or idea from [author]'s device; returns its id. */
    public UUID send(CurrentUser author, String kind, String rawMessage) {
        if (kind == null || !KINDS.contains(kind)) {
            throw ApiException.badRequest("invalid_kind");
        }
        String message = rawMessage == null ? "" : rawMessage.strip();
        if (message.isEmpty() || message.codePointCount(0, message.length()) > MAX_MESSAGE_LENGTH) {
            throw ApiException.badRequest("invalid_message");
        }
        return transactions.inTransaction(() -> feedback.create(author.id(), author.deviceId(), kind, message));
    }

    /** Newest first; [openOnly] leaves out what was already handled. */
    public List<FeedbackItem> inbox(boolean openOnly) {
        return transactions.inTransaction(() -> feedback.list(openOnly, LIST_LIMIT));
    }

    /** Marks [id] resolved or open again ([resolved] null changes nothing); 404 if there is no such item. */
    public FeedbackItem setResolved(UUID id, Boolean resolved) {
        return transactions.inTransaction(() -> {
            if (resolved != null) {
                feedback.setResolved(id, resolved);
            }
            return feedback.find(id).orElseThrow(ApiException::notFound);
        });
    }
}
