package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class MigrationScriptsTest {

    @Test
    void theLatestVersionComparesNumbersNotText() {
        assertThat(MigrationScripts.latestOf(List.of("V1__a.sql", "V2__b.sql", "V10__c.sql", "V9__d.sql")))
                .isEqualTo("10");
    }

    @Test
    void dottedVersionsCompareEachPartAsANumber() {
        assertThat(MigrationScripts.latestOf(List.of("V1.2__a.sql", "V1.10__b.sql", "V1.9__c.sql")))
                .isEqualTo("1.10");
    }

    @Test
    void namesThatAreNotVersionedMigrationsAreIgnored() {
        assertThat(MigrationScripts.latestOf(List.of("R__views.sql", "README.md", "V3__c.sql", "V4_missing_separator.sql")))
                .isEqualTo("3");
    }

    @Test
    void noScriptsMeansNoVersion() {
        assertThat(MigrationScripts.latestOf(List.of())).isNull();
    }

    @Test
    void theBundledScriptsEndAtTheSeedMigration() {
        assertThat(MigrationScripts.latestVersion()).isEqualTo("2");
    }
}
