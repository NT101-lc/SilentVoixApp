package com.silentvoix.backend.feedback;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.auth.CurrentUser;
import com.silentvoix.backend.database.Jdbc;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/feedback}: any signed-in user reports a wrong result, a bug or an idea. */
@RestController
@RequestMapping("/api/v1/feedback")
class FeedbackController {

    static final Set<String> KINDS = Set.of("wrong_result", "bug", "idea");
    static final int MAX_MESSAGE_LENGTH = 2000;

    record FeedbackRequest(String kind, String message) {
    }

    record Created(UUID id) {
    }

    private final Jdbc jdbc;

    FeedbackController(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Created send(CurrentUser user, @RequestBody FeedbackRequest request) {
        if (request.kind() == null || !KINDS.contains(request.kind())) {
            throw ApiException.badRequest("invalid_kind");
        }
        String message = request.message() == null ? "" : request.message().strip();
        if (message.isEmpty() || message.codePointCount(0, message.length()) > MAX_MESSAGE_LENGTH) {
            throw ApiException.badRequest("invalid_message");
        }
        return jdbc.transaction(connection -> {
            try (PreparedStatement insert = Jdbc.prepare(connection,
                    "INSERT INTO feedback (user_id, device_id, kind, message) VALUES (?, ?, ?, ?) RETURNING id",
                    user.id(), user.deviceId(), request.kind(), message);
                 ResultSet rows = insert.executeQuery()) {
                rows.next();
                return new Created(rows.getObject(1, UUID.class));
            }
        });
    }
}
