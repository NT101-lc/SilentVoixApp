package com.silentvoix.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import javax.sql.DataSource;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * {@code GET /api/v1/health} through the full Spring context, one nested class per database state.
 * Every state must answer 200 with the same contract: status, service, timestamp, database.
 */
class HealthEndpointTest {

    private static ResultActions assertContract(ResultActions result) throws Exception {
        String body = result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("silentvoix-backend"))
                .andReturn().getResponse().getContentAsString();
        // Parses, so the timestamp is a real ISO-8601 instant.
        assertThat(Instant.parse(JsonPath.read(body, "$.timestamp"))).isNotNull();
        return result;
    }

    /** No SILENTVOIX_DATABASE_URL: the app still starts and reports the database as not configured. */
    @Nested
    @SpringBootTest(properties = "silentvoix.database.url=")
    @AutoConfigureMockMvc
    class NotConfigured {

        @Autowired
        MockMvc mvc;

        @Test
        void degradedWithDatabaseNotConfigured() throws Exception {
            assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("DEGRADED"))
                    .andExpect(jsonPath("$.database.status").value("NOT_CONFIGURED"))
                    .andExpect(jsonPath("$.database.latencyMs").value(nullValue()))
                    .andExpect(jsonPath("$.database.error").value(nullValue()));
        }
    }

    /** A reachable, valid database. */
    @Nested
    @SpringBootTest(properties = "silentvoix.database.url=")
    @AutoConfigureMockMvc
    class Up {

        @TestConfiguration
        static class HealthyDatabase {
            @Bean
            DataSource dataSource() {
                return FakeDataSource.healthy();
            }
        }

        @Autowired
        MockMvc mvc;

        @Test
        void upWithLatencyAndNoError() throws Exception {
            assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.database.status").value("UP"))
                    .andExpect(jsonPath("$.database.latencyMs").isNumber())
                    .andExpect(jsonPath("$.database.error").value(nullValue()));
        }
    }

    /**
     * The real Hikari pool and PostgreSQL driver pointed at a closed local port, with credentials in
     * both the URL and the settings. None of them may appear anywhere in the response.
     */
    @Nested
    @SpringBootTest(properties = {
            "silentvoix.database.url=jdbc:postgresql://127.0.0.1:1/silentvoix_db?password=url-s3cr3t",
            "silentvoix.database.username=neondb_owner",
            "silentvoix.database.password=prop-s3cr3t",
            "silentvoix.database.connection-timeout=1s",
    })
    @AutoConfigureMockMvc
    class Down {

        @Autowired
        MockMvc mvc;

        @Test
        void degradedWithACredentialFreeError() throws Exception {
            String body = assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("DEGRADED"))
                    .andExpect(jsonPath("$.database.status").value("DOWN"))
                    .andExpect(jsonPath("$.database.latencyMs").value(nullValue()))
                    .andExpect(jsonPath("$.database.error").isString())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain(
                    "url-s3cr3t", "prop-s3cr3t", "neondb_owner", "127.0.0.1", "silentvoix_db", "jdbc:");
        }
    }
}
