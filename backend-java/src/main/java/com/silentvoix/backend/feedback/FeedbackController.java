package com.silentvoix.backend.feedback;

import java.util.UUID;

import com.silentvoix.backend.auth.CurrentUser;
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

    record FeedbackRequest(String kind, String message) {
    }

    record Created(UUID id) {
    }

    private final FeedbackService feedback;

    FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Created send(CurrentUser user, @RequestBody FeedbackRequest request) {
        return new Created(feedback.send(user, request.kind(), request.message()));
    }
}
