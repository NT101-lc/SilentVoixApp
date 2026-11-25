package com.silentvoix.backend.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.database.DuplicateKeyException;
import com.silentvoix.backend.database.Transactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * E-mail + password accounts and per-device sessions (the logic layer: rules here, storage in
 * {@link AccountRepository} and {@link SessionRepository}). A session is a random token whose
 * SHA-256 is stored against a device; it lasts {@link #SESSION_LIFETIME} and is checked against
 * the database on every request.
 */
@Service
public class AuthService {

    static final Duration SESSION_LIFETIME = Duration.ofDays(30);

    /** A signed-in session handed to the app. */
    public record Session(String token, Instant expiresAt, Account user) {
    }

    private final Transactions transactions;
    private final AccountRepository accounts;
    private final SessionRepository sessions;
    private final PasswordHasher hasher;
    private final LoginThrottle throttle;
    private final Clock clock;
    /** Compared against when the address is unknown, so both failures take as long. */
    private final String decoyHash;

    @Autowired
    AuthService(Transactions transactions, AccountRepository accounts, SessionRepository sessions,
                PasswordHasher hasher, LoginThrottle throttle) {
        this(transactions, accounts, sessions, hasher, throttle, Clock.systemUTC());
    }

    AuthService(Transactions transactions, AccountRepository accounts, SessionRepository sessions,
                PasswordHasher hasher, LoginThrottle throttle, Clock clock) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.sessions = sessions;
        this.hasher = hasher;
        this.throttle = throttle;
        this.clock = clock;
        this.decoyHash = hasher.hash("silentvoix-decoy-password");
    }

    /** A new ordinary user (never an admin) with a password, signed in on the calling device. */
    public Session register(String rawEmail, String password, String rawName, String platform, String appVersion) {
        String email = Credentials.email(rawEmail);
        String hash = hasher.hash(Credentials.newPassword(password));
        String name = Credentials.displayName(rawName);
        String devicePlatform = Credentials.platform(platform);
        String version = Credentials.appVersion(appVersion);
        return transactions.inTransaction(() -> {
            UUID userId = accounts.createUser(name);
            try {
                accounts.addEmailIdentity(userId, email);
            } catch (DuplicateKeyException e) {
                throw new ApiException(HttpStatus.CONFLICT, "email_taken");
            }
            accounts.addPasswordHash(userId, hash);
            return openSession(new Account(userId, email, name, CurrentUser.ROLE_USER), devicePlatform, version);
        });
    }

    /**
     * Signs in with the address's password. A wrong password and an unknown address get the same
     * answer; a locked account is named only to someone who knows its password.
     */
    public Session login(String rawEmail, String password, String platform, String appVersion) {
        String email;
        try {
            email = Credentials.email(rawEmail);
        } catch (ApiException e) {
            throw invalidCredentials();
        }
        String devicePlatform = Credentials.platform(platform);
        String version = Credentials.appVersion(appVersion);
        if (throttle.isBlocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
        }
        return transactions.inTransaction(() -> {
            Optional<AccountRepository.LoginRecord> found = accounts.findByEmail(email);
            // An unknown address, or one with no password on file (OTP only), still costs one comparison.
            String hash = found.map(AccountRepository.LoginRecord::passwordHash).orElse(null);
            boolean correct = hasher.matches(password, hash != null ? hash : decoyHash) && hash != null;
            if (!correct) {
                throttle.recordFailure(email);
                throw invalidCredentials();
            }
            throttle.recordSuccess(email);
            AccountRepository.LoginRecord login = found.get();
            if (login.locked()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled");
            }
            return openSession(login.account(), devicePlatform, version);
        });
    }

    /** The user behind [token], or null if it is unknown, revoked, expired or the account is locked. */
    public CurrentUser authenticate(String token) {
        byte[] hash = SessionTokens.hash(token);
        return transactions.inTransaction(() -> {
            Optional<CurrentUser> user = sessions.findActive(hash);
            // At most one write a minute per device, not one per request.
            user.ifPresent(found -> sessions.touchDevice(found.deviceId()));
            return user.orElse(null);
        });
    }

    public void logout(CurrentUser user) {
        transactions.run(() -> sessions.revoke(user.sessionId()));
    }

    private Session openSession(Account account, String platform, String appVersion) {
        UUID deviceId = sessions.addDevice(account.id(), platform, appVersion);
        String token = SessionTokens.newToken();
        Instant expiresAt = clock.instant().plus(SESSION_LIFETIME);
        sessions.addSession(deviceId, SessionTokens.hash(token), expiresAt);
        return new Session(token, expiresAt, account);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
    }
}
