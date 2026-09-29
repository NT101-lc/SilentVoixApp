package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class DatabaseConfigurationTest {

    // Neon's dashboard shows this form (not JDBC), with the password embedded.
    private static final DatabaseProperties NEON_STYLE_URL = new DatabaseProperties(
            "postgresql://neondb_owner:npg_s3cr3t@ep-cool-darkness-123456-pooler.eu-central-1.aws.neon.tech/neondb",
            "neondb_owner", "npg_s3cr3t", 5, Duration.ofSeconds(10));

    @Test
    void aNonJdbcUrlIsRejectedWithoutEchoingIt() {
        assertThatThrownBy(() -> new DatabaseConfiguration().dataSource(NEON_STYLE_URL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:postgresql://")
                .message().doesNotContain("npg_s3cr3t", "neon.tech");
    }

    @Test
    void propertiesNeverPrintTheUrlOrPassword() {
        assertThat(NEON_STYLE_URL.toString()).doesNotContain("npg_s3cr3t", "neon.tech");
    }
}
