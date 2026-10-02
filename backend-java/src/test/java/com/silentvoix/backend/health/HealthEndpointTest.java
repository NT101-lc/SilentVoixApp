package com.silentvoix.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import javax.sql.DataSource;

import com.jayway.jsonpath.JsonPath;
import com.silentvoix.backend.database.MigrationScripts;
import com.silentvoix.backend.database.TestDatabase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

    /** A reachable database migrated to the latest bundled script. */
    @Nested
    @SpringBootTest(properties = {"silentvoix.database.url=", "silentvoix.database.migrate-on-startup=false"})
    @AutoConfigureMockMvc
    class Up {

        @TestConfiguration
        static class HealthyDatabase {
            @Bean
            DataSource dataSource() {
                return FakeDataSource.migratedTo(MigrationScripts.latestVersion());
            }
        }

        @Autowired
        MockMvc mvc;

        @Test
        void upWithLatencySchemaVersionAndNoError() throws Exception {
            assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.database.status").value("UP"))
                    .andExpect(jsonPath("$.database.latencyMs").isNumber())
                    .andExpect(jsonPath("$.database.schemaVersion").value(MigrationScripts.latestVersion()))
                    .andExpect(jsonPath("$.database.error").value(nullValue()));
        }
    }

    /** Reachable, but the schema is behind the scripts this build ships: not ready to serve data. */
    @Nested
    @SpringBootTest(properties = {"silentvoix.database.url=", "silentvoix.database.migrate-on-startup=false"})
    @AutoConfigureMockMvc
    class SchemaBehind {

        @TestConfiguration
        static class OldSchema {
            @Bean
            DataSource dataSource() {
                return FakeDataSource.migratedTo("1");
            }
        }

        @Autowired
        MockMvc mvc;

        @Test
        void degradedWhileTheDatabaseItselfIsUp() throws Exception {
            assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("DEGRADED"))
                    .andExpect(jsonPath("$.database.status").value("UP"))
                    .andExpect(jsonPath("$.database.schemaVersion").value("1"));
        }
    }

    /** Never migrated (no Flyway history table): reported as no schema at all. */
    @Nested
    @SpringBootTest(properties = {"silentvoix.database.url=", "silentvoix.database.migrate-on-startup=false"})
    @AutoConfigureMockMvc
    class NoSchema {

        @TestConfiguration
        static class EmptyDatabase {
            @Bean
            DataSource dataSource() {
                return FakeDataSource.healthy();
            }
        }

        @Autowired
        MockMvc mvc;

        @Test
        void degradedWithANullSchemaVersion() throws Exception {
            assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("DEGRADED"))
                    .andExpect(jsonPath("$.database.status").value("UP"))
                    .andExpect(jsonPath("$.database.schemaVersion").value(nullValue()));
        }
    }

    /**
     * End to end: a real PostgreSQL, configured exactly as on Railway (URL, user, password). Startup
     * migrates it, and health then reports UP at the latest schema version.
     */
    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ExtendWith(OutputCaptureExtension.class)
    class RealDatabase {

        private static final TestDatabase.Database DATABASE = TestDatabase.fresh();

        @DynamicPropertySource
        static void database(DynamicPropertyRegistry registry) {
            registry.add("silentvoix.database.url", DATABASE::jdbcUrl);
            registry.add("silentvoix.database.username", DATABASE::username);
            registry.add("silentvoix.database.password", DATABASE::password);
        }

        @Autowired
        MockMvc mvc;

        @Test
        void startupMigratesAndHealthIsUp() throws Exception {
            assertContract(mvc.perform(get("/api/v1/health")))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.database.status").value("UP"))
                    .andExpect(jsonPath("$.database.schemaVersion").value(MigrationScripts.latestVersion()));
        }

        @Test
        void startupLogsNeverNameTheDatabase(CapturedOutput output) {
            // Flyway announces the database it connects to; that line must not carry the URL.
            assertThat(output.toString())
                    .contains("Database schema at version")
                    .doesNotContain(DATABASE.jdbcUrl(), "jdbc:postgresql://");
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
