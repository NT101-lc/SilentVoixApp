package com.silentvoix.backend.database;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Migrates the database once the application has started. A failure (Neon unreachable, a bad
 * script) is logged and the app keeps serving: the health endpoint then reports the schema as
 * behind, and the next start tries again. Disable with
 * {@code silentvoix.database.migrate-on-startup=false}.
 */
@Component
@ConditionalOnProperty(name = "silentvoix.database.migrate-on-startup", havingValue = "true", matchIfMissing = true)
class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final ObjectProvider<DataSource> dataSource;
    private final SchemaMigrator migrator;

    SchemaMigrationRunner(ObjectProvider<DataSource> dataSource, SchemaMigrator migrator) {
        this.dataSource = dataSource;
        this.migrator = migrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        DataSource source = dataSource.getIfAvailable();
        if (source == null) {
            return;
        }
        try {
            SchemaMigrator.Outcome outcome = migrator.migrate(source);
            log.info("Database schema at version {} ({} migration(s) applied)", outcome.version(), outcome.applied());
        } catch (RuntimeException e) {
            // Messages from the driver or Flyway can name the host, database or user: log the kind only.
            log.error("Schema migration failed ({}); health will report the schema as not applied", describe(e));
        }
    }

    private static String describe(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null) {
                return failure.getClass().getSimpleName() + ", SQLState " + sql.getSQLState();
            }
        }
        return failure.getClass().getSimpleName();
    }
}
