package com.silentvoix.backend.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt (cost 10) for stored passwords. Compatible with pgcrypto's {@code crypt(pw, gen_salt('bf'))},
 * which {@code scripts/seed_accounts.sql} uses.
 */
@Component
public class PasswordHasher {

    private static final int COST = 10;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    public String hash(String password) {
        return encoder.encode(password);
    }

    /** False for a missing or malformed hash, never an exception. */
    public boolean matches(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return encoder.matches(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
