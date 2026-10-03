package com.silentvoix.backend.auth;

import static com.silentvoix.backend.auth.ApiClient.PASSWORD;
import static com.silentvoix.backend.auth.ApiClient.body;
import static com.silentvoix.backend.auth.ApiClient.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import com.silentvoix.backend.database.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sign-up, sign-in, the current user and sign-out, through the full context against a real,
 * migrated PostgreSQL. Each test uses its own e-mail address.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    private static final TestDatabase.Database DATABASE = TestDatabase.fresh();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("silentvoix.database.url", DATABASE::jdbcUrl);
        registry.add("silentvoix.database.username", DATABASE::username);
        registry.add("silentvoix.database.password", DATABASE::password);
    }

    @Autowired
    MockMvc mvc;

    ApiClient api;

    @BeforeEach
    void client() {
        api = new ApiClient(mvc, DATABASE);
    }

    @Nested
    class Register {

        @Test
        void createsAnOrdinaryUserAndSignsThemIn() throws Exception {
            var result = api.register("lan@example.com", PASSWORD, "Lan")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.user.email").value("lan@example.com"))
                    .andExpect(jsonPath("$.user.displayName").value("Lan"))
                    .andExpect(jsonPath("$.user.role").value("user"));

            Instant expires = Instant.parse(JsonPath.read(body(result), "$.expiresAt"));
            assertThat(Duration.between(Instant.now(), expires)).isBetween(Duration.ofDays(29), Duration.ofDays(31));
        }

        @Test
        void theAddressIsStoredTrimmedAndInLowerCase() throws Exception {
            api.register("  Minh@Example.COM ", PASSWORD, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.email").value("minh@example.com"))
                    .andExpect(jsonPath("$.user.displayName").value(nullValue()));
        }

        @Test
        void nobodyCanMakeThemselvesAnAdmin() throws Exception {
            api.post("/api/v1/auth/register", null, """
                    {"email": "sneaky@example.com", "password": "%s", "role": "admin"}""".formatted(PASSWORD))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.role").value("user"));
        }

        @Test
        void anAddressCanBeRegisteredOnlyOnce() throws Exception {
            api.signUp("twice@example.com");

            api.register("TWICE@example.com", PASSWORD, "Lan")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("email_taken"));
        }

        @Test
        void rejectsAMalformedAddress() throws Exception {
            api.register("not-an-email", PASSWORD, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_email"));
        }

        @Test
        void rejectsAPasswordShorterThanEightCharacters() throws Exception {
            api.register("short@example.com", "1234567", null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("password_too_short"));
        }

        @Test
        void rejectsAPasswordLongerThanBcryptCanHash() throws Exception {
            // 25 × "ấ" is 25 characters but 75 bytes in UTF-8; BCrypt reads at most 72.
            api.register("long@example.com", "ấ".repeat(25), null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("password_too_long"));
        }

        @Test
        void rejectsANameLongerThan80Characters() throws Exception {
            api.register("named@example.com", PASSWORD, "x".repeat(81))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_display_name"));
        }

        @Test
        void storesOnlyAHashOfThePasswordAndOfTheToken() throws Exception {
            String token = token(api.register("hashed@example.com", PASSWORD, null));

            String stored = (String) api.scalar("""
                    SELECT p.password_hash FROM password_credential p
                    JOIN user_identity i ON i.user_id = p.user_id WHERE i.subject = 'hashed@example.com'""");
            assertThat(stored).startsWith("$2").doesNotContain(PASSWORD);
            assertThat(api.scalar("SELECT count(*) FROM refresh_token WHERE token_hash = ?", SessionTokens.hash(token)))
                    .isEqualTo(1L);
        }
    }

    @Nested
    class Login {

        @Test
        void theRightPasswordGivesANewSession() throws Exception {
            String first = api.signUp("login@example.com");

            String second = token(api.login("LOGIN@example.com", PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").value("login@example.com"))
                    .andExpect(jsonPath("$.user.role").value("user")));

            assertThat(second).isNotEqualTo(first);
        }

        @Test
        void aWrongPasswordAndAnUnknownAddressGetTheSameAnswer() throws Exception {
            api.signUp("known@example.com");

            String wrongPassword = body(api.login("known@example.com", "wrong-password").andExpect(status().isUnauthorized()));
            String unknown = body(api.login("nobody@example.com", PASSWORD).andExpect(status().isUnauthorized()));

            assertThat(wrongPassword).isEqualTo(unknown).contains("\"invalid_credentials\"");
        }

        @Test
        void anAccountWithoutAPasswordCannotSignInWithOne() throws Exception {
            api.sql("""
                    WITH u AS (INSERT INTO app_user DEFAULT VALUES RETURNING id)
                    INSERT INTO user_identity (user_id, provider, subject) SELECT id, 'email', 'otp-only@example.com' FROM u""");

            api.login("otp-only@example.com", PASSWORD)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_credentials"));
        }

        @Test
        void aLockedAccountIsToldSoOnlyWithTheRightPassword() throws Exception {
            api.signUp("locked@example.com");
            api.sql("""
                    UPDATE app_user SET disabled_at = now()
                    WHERE id = (SELECT user_id FROM user_identity WHERE subject = 'locked@example.com')""");

            api.login("locked@example.com", "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_credentials"));
            api.login("locked@example.com", PASSWORD)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("account_disabled"));
        }

        @Test
        void fiveWrongPasswordsBlockTheAddressForAWhile() throws Exception {
            api.signUp("guessed@example.com");
            for (int i = 0; i < 5; i++) {
                api.login("guessed@example.com", "guess-" + i).andExpect(status().isUnauthorized());
            }

            // Even the right password waits now, so guessing cannot continue.
            api.login("guessed@example.com", PASSWORD)
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("too_many_attempts"));
        }
    }

    @Nested
    class Session {

        @Test
        void meDescribesTheSignedInUser() throws Exception {
            String token = api.signUp("me@example.com");

            api.get("/api/v1/auth/me", token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.email").value("me@example.com"))
                    .andExpect(jsonPath("$.displayName").value("Lan"))
                    .andExpect(jsonPath("$.role").value("user"));
        }

        @Test
        void meFollowsARoleChangeMadeOnTheServer() throws Exception {
            String token = api.signUpAdmin("promoted@example.com");

            api.get("/api/v1/auth/me", token).andExpect(jsonPath("$.role").value("admin"));
        }

        @Test
        void withoutAValidTokenTheAnswerIs401() throws Exception {
            api.get("/api/v1/auth/me", null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("unauthorized"));
            api.get("/api/v1/auth/me", "made-up-token")
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void anExpiredSessionNoLongerWorks() throws Exception {
            String token = api.signUp("expired@example.com");
            api.sql("UPDATE refresh_token SET created_at = now() - interval '31 days', expires_at = now() - interval '1 day' WHERE token_hash = ?",
                    SessionTokens.hash(token));

            api.get("/api/v1/auth/me", token).andExpect(status().isUnauthorized());
        }

        @Test
        void signingOutEndsOnlyThatSession() throws Exception {
            String phone = api.signUp("two-devices@example.com");
            String tablet = token(api.login("two-devices@example.com", PASSWORD));

            api.post("/api/v1/auth/logout", phone, "{}").andExpect(status().isNoContent());

            api.get("/api/v1/auth/me", phone).andExpect(status().isUnauthorized());
            api.get("/api/v1/auth/me", tablet).andExpect(status().isOk());
        }

        @Test
        void lockingAnAccountEndsItsSessionsAtOnce() throws Exception {
            String token = api.signUp("locked-later@example.com");
            api.sql("""
                    UPDATE app_user SET disabled_at = now()
                    WHERE id = (SELECT user_id FROM user_identity WHERE subject = 'locked-later@example.com')""");

            api.get("/api/v1/auth/me", token).andExpect(status().isUnauthorized());
        }

        @Test
        void healthStaysOpenWithoutAToken() throws Exception {
            api.get("/api/v1/health", null).andExpect(status().isOk());
        }
    }
}
