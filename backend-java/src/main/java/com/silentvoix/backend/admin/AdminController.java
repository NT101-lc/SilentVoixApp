package com.silentvoix.backend.admin;

import java.util.List;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.auth.CurrentUser;
import com.silentvoix.backend.feedback.FeedbackItem;
import com.silentvoix.backend.feedback.FeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/admin}: admins only (the session guard answers 403 to anyone else). The overview,
 * account management (role, lock) and the feedback inbox. Only HTTP mapping lives here; the rules
 * are in {@link AdminService} and {@link FeedbackService}.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    /** Either field may be left out. */
    record UserChange(String role, Boolean locked) {
    }

    record FeedbackChange(Boolean resolved) {
    }

    private final AdminService admin;
    private final FeedbackService feedback;

    AdminController(AdminService admin, FeedbackService feedback) {
        this.admin = admin;
        this.feedback = feedback;
    }

    @GetMapping("/overview")
    Overview overview() {
        return admin.overview();
    }

    @GetMapping("/users")
    List<AdminUser> users() {
        return admin.users();
    }

    @PatchMapping("/users/{id}")
    AdminUser changeUser(CurrentUser user, @PathVariable String id, @RequestBody UserChange change) {
        return admin.changeUser(user, uuid(id), change.role(), change.locked());
    }

    /** Newest first; {@code status=open} leaves out what was already handled. */
    @GetMapping("/feedback")
    List<FeedbackItem> feedback(@RequestParam(defaultValue = "all") String status) {
        return feedback.inbox("open".equals(status));
    }

    @PatchMapping("/feedback/{id}")
    FeedbackItem changeFeedback(@PathVariable String id, @RequestBody FeedbackChange change) {
        return feedback.setResolved(uuid(id), change.resolved());
    }

    /** A malformed id names nothing, so it is a 404 like an unknown one. */
    private static UUID uuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw ApiException.notFound();
        }
    }
}
