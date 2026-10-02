package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.sql.DataSource;

@ExtendWith(OutputCaptureExtension.class)
class SchemaMigrationRunnerTest {

    private static SchemaMigrationRunner runnerFor(DataSource dataSource) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (dataSource != null) {
            beans.addBean("dataSource", dataSource);
        }
        return new SchemaMigrationRunner(beans.getBeanProvider(DataSource.class), new SchemaMigrator());
    }

    @Test
    void withoutADatabaseThereIsNothingToMigrate() {
        assertThatCode(() -> runnerFor(null).run(null)).doesNotThrowAnyException();
    }

    @Test
    void anUnreachableDatabaseDoesNotStopStartupAndNothingSecretIsLogged(CapturedOutput output) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://127.0.0.1:1/silentvoix_db?password=url-s3cr3t");
        config.setUsername("neondb_owner");
        config.setPassword("prop-s3cr3t");
        config.setConnectionTimeout(1_000);
        config.setInitializationFailTimeout(-1);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThatCode(() -> runnerFor(dataSource).run(null)).doesNotThrowAnyException();
        }

        assertThat(output).contains("Schema migration failed");
        assertThat(output.toString()).doesNotContain("url-s3cr3t", "prop-s3cr3t", "silentvoix_db");
    }

    @Test
    void aReachableDatabaseIsMigratedAtStartup() {
        try (HikariDataSource dataSource = TestDatabase.fresh().dataSource()) {
            runnerFor(dataSource).run(null);

            assertThat(new SchemaMigrator().migrate(dataSource).applied()).as("nothing left to apply").isZero();
        }
    }
}
