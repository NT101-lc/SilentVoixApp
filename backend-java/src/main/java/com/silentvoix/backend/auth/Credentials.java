package com.silentvoix.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.silentvoix.backend.api.ApiException;

/** Checks and normalises what a person types to sign up or in. */
final class Credentials {

    static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt reads only the first 72 bytes; a longer password would be silently cut. */
    static final int MAX_PASSWORD_BYTES = 72;
    private static final int MAX_EMAIL_LENGTH = 320;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_APP_VERSION_LENGTH = 40;
    private static final Set<String> PLATFORMS = Set.of("android", "ios", "web");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private Credentials() {
    }

    /** Trimmed and lower-cased, as {@code user_identity.subject} stores it. */
    static String email(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("invalid_email");
        }
        return email;
    }

    static String newPassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("password_too_short");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw ApiException.badRequest("password_too_long");
        }
        return password;
    }

    /** Trimmed; blank means no name. */
    static String displayName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim();
        if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            throw ApiException.badRequest("invalid_display_name");
        }
        return name;
    }

    static String platform(String raw) {
        String platform = raw == null || raw.isBlank() ? "android" : raw.trim().toLowerCase(Locale.ROOT);
        if (!PLATFORMS.contains(platform)) {
            throw ApiException.badRequest("invalid_device");
        }
        return platform;
    }

    static String appVersion(String raw) {
        String version = raw == null || raw.isBlank() ? "unknown" : raw.trim();
        if (version.length() > MAX_APP_VERSION_LENGTH) {
            throw ApiException.badRequest("invalid_device");
        }
        return version;
    }
}
