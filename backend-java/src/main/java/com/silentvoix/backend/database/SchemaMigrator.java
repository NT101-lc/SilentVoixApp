package com.silentvoix.backend.database;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Component;

/** Brings a database up to the bundled schema with Flyway (scripts in {@code db/migration}). */
@Component
public class SchemaMigrator {

    /** How many scripts this run applied, and the schema version afterwards (null if none). */
    public record Outcome(int applied, String version) {
    }

    public Outcome migrate(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(MigrationScripts.LOCATION)
                .load();
        MigrateResult result = flyway.migrate();
        MigrationInfo current = flyway.info().current();
        return new Outcome(result.migrationsExecuted, current != null ? current.getVersion().getVersion() : null);
    }
}
