package com.silentvoix.backend.admin;

import static com.silentvoix.backend.auth.ApiClient.PASSWORD;
import static com.silentvoix.backend.auth.ApiClient.body;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.silentvoix.backend.auth.ApiClient;
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
 * What only an admin may do: see the overview, manage accounts and work through feedback. Users
 * send feedback; admins read it. Against a real, migrated PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiTest {

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
    class Access {

        @Test
        void anOrdinaryUserIsRefusedEveryAdminCall() throws Exception {
            String user = api.signUp("plain@example.com");

            api.get("/api/v1/admin/overview", user).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("forbidden"));
            api.get("/api/v1/admin/users", user).andExpect(status().isForbidden());
            api.get("/api/v1/admin/feedback", user).andExpect(status().isForbidden());
            api.patch("/api/v1/admin/users/" + api.userId(user), user, "{\"role\": \"admin\"}")
                    .andExpect(status().isForbidden());
        }

        @Test
        void withoutATokenTheAnswerIs401NotForbidden() throws Exception {
            api.get("/api/v1/admin/overview", null).andExpect(status().isUnauthorized());
        }

        @Test
        void aDemotedAdminLosesAccessOnTheNextCall() throws Exception {
            String admin = api.signUpAdmin("demoted@example.com");
            api.get("/api/v1/admin/overview", admin).andExpect(status().isOk());

            api.sql("UPDATE app_user SET role = 'user' WHERE id = ?::uuid", api.userId(admin));

            api.get("/api/v1/admin/overview", admin).andExpect(status().isForbidden());
        }
    }

    @Nested
    class Overview {

        @Test
        void countsAccountsSessionsAndOpenFeedback() throws Exception {
            String admin = api.signUpAdmin("counter@example.com");
            String user = api.signUp("counted@example.com");
            api.post("/api/v1/feedback", user, "{\"kind\": \"idea\", \"message\": \"Thêm ký hiệu\"}");

            api.get("/api/v1/admin/overview", admin)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.admins").value(greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.lockedUsers").isNumber())
                    .andExpect(jsonPath("$.newUsersThisWeek").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.activeUsersThisWeek").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.openFeedback").value(greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    class Users {

        @Test
        void listsEveryAccountWithItsRoleAndState() throws Exception {
            String admin = api.signUpAdmin("lister@example.com");
            api.signUp("listed@example.com");

            String body = body(api.get("/api/v1/admin/users", admin).andExpect(status().isOk()));

            org.assertj.core.api.Assertions.assertThat(JsonPath.<java.util.List<String>>read(body,
                    "$[?(@.email == 'listed@example.com')].role")).containsExactly("user");
            org.assertj.core.api.Assertions.assertThat(JsonPath.<java.util.List<Boolean>>read(body,
                    "$[?(@.email == 'listed@example.com')].locked")).containsExactly(false);
            org.assertj.core.api.Assertions.assertThat(JsonPath.<java.util.List<Object>>read(body,
                    "$[?(@.email == 'listed@example.com')].lastSeenAt")).hasSize(1).doesNotContainNull();
        }

        @Test
        void anAdminCanPromoteAUser() throws Exception {
            String admin = api.signUpAdmin("promoter@example.com");
            String user = api.signUp("rising@example.com");

            api.patch("/api/v1/admin/users/" + api.userId(user), admin, "{\"role\": \"admin\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("admin"));
            api.get("/api/v1/admin/overview", user).andExpect(status().isOk());
        }

        @Test
        void lockingSignsTheUserOutAndKeepsThemOut() throws Exception {
            String admin = api.signUpAdmin("locker@example.com");
            String user = api.signUp("troublemaker@example.com");

            api.patch("/api/v1/admin/users/" + api.userId(user), admin, "{\"locked\": true}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(true));

            api.get("/api/v1/auth/me", user).andExpect(status().isUnauthorized());
            api.login("troublemaker@example.com", PASSWORD).andExpect(status().isForbidden());
        }

        @Test
        void unlockingLetsThemSignInAgain() throws Exception {
            String admin = api.signUpAdmin("unlocker@example.com");
            String user = api.signUp("forgiven@example.com");
            String id = api.userId(user);
            api.patch("/api/v1/admin/users/" + id, admin, "{\"locked\": true}");

            api.patch("/api/v1/admin/users/" + id, admin, "{\"locked\": false}")
                    .andExpect(jsonPath("$.locked").value(false));

            api.login("forgiven@example.com", PASSWORD).andExpect(status().isOk());
        }

        @Test
        void anAdminCannotChangeTheirOwnAccount() throws Exception {
            // So the last admin can never lock or demote themselves out of the app.
            String admin = api.signUpAdmin("self@example.com");

            api.patch("/api/v1/admin/users/" + api.userId(admin), admin, "{\"role\": \"user\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("cannot_change_self"));
            api.patch("/api/v1/admin/users/" + api.userId(admin), admin, "{\"locked\": true}")
                    .andExpect(status().isConflict());
        }

        @Test
        void anUnknownRoleOrUserIsRejected() throws Exception {
            String admin = api.signUpAdmin("strict@example.com");
            String user = api.signUp("target@example.com");

            api.patch("/api/v1/admin/users/" + api.userId(user), admin, "{\"role\": \"owner\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_role"));
            api.patch("/api/v1/admin/users/00000000-0000-0000-0000-000000000000", admin, "{\"locked\": true}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("not_found"));
            api.patch("/api/v1/admin/users/not-a-uuid", admin, "{\"locked\": true}")
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Feedback {

        @Test
        void aUserSendsFeedbackAndAnAdminSeesWhoSentIt() throws Exception {
            String admin = api.signUpAdmin("reader@example.com");
            String user = api.signUp("writer@example.com");

            api.post("/api/v1/feedback", user, "{\"kind\": \"wrong_result\", \"message\": \"  Nhận nhầm 'Xin chào'  \"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isString());

            String inbox = body(api.get("/api/v1/admin/feedback", admin).andExpect(status().isOk()));
            org.assertj.core.api.Assertions.assertThat(JsonPath.<java.util.List<String>>read(inbox,
                    "$[?(@.authorEmail == 'writer@example.com')].message")).containsExactly("Nhận nhầm 'Xin chào'");
            org.assertj.core.api.Assertions.assertThat(JsonPath.<java.util.List<String>>read(inbox,
                    "$[?(@.authorEmail == 'writer@example.com')].kind")).containsExactly("wrong_result");
        }

        @Test
        void feedbackNeedsAKnownKindAndAMessage() throws Exception {
            String user = api.signUp("careless@example.com");

            api.post("/api/v1/feedback", user, "{\"kind\": \"praise\", \"message\": \"Hay\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_kind"));
            api.post("/api/v1/feedback", user, "{\"kind\": \"idea\", \"message\": \"   \"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_message"));
            api.post("/api/v1/feedback", user, "{\"kind\": \"idea\", \"message\": \"" + "x".repeat(2001) + "\"}")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void sendingFeedbackNeedsASignedInUser() throws Exception {
            api.post("/api/v1/feedback", null, "{\"kind\": \"idea\", \"message\": \"Hay\"}")
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void resolvedFeedbackLeavesTheOpenInbox() throws Exception {
            String admin = api.signUpAdmin("resolver@example.com");
            String user = api.signUp("reporter@example.com");
            String id = JsonPath.read(body(api.post("/api/v1/feedback", user,
                    "{\"kind\": \"bug\", \"message\": \"Ứng dụng bị treo\"}")), "$.id");

            api.patch("/api/v1/admin/feedback/" + id, admin, "{\"resolved\": true}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resolvedAt").isString());

            api.get("/api/v1/admin/feedback?status=open", admin)
                    .andExpect(jsonPath("$[*].id", not(hasItem(id))));
            api.get("/api/v1/admin/feedback", admin)
                    .andExpect(jsonPath("$[*].id", hasItem(id)));

            api.patch("/api/v1/admin/feedback/" + id, admin, "{\"resolved\": false}")
                    .andExpect(jsonPath("$.resolvedAt").value(nullValue()));
        }
    }
}
