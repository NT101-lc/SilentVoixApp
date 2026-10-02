package com.silentvoix.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class DatabaseHealthCheckerTest {

    private static DatabaseHealthChecker checkerFor(DataSource dataSource) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (dataSource != null) {
            beans.addBean("dataSource", dataSource);
        }
        return new DatabaseHealthChecker(beans.getBeanProvider(DataSource.class));
    }

    @Test
    void notConfiguredWhenThereIsNoDataSource() {
        DatabaseHealth health = checkerFor(null).check();

        assertThat(health).isEqualTo(new DatabaseHealth(DatabaseHealth.Status.NOT_CONFIGURED, null, null, null));
    }

    @Test
    void upWithLatencyWhenAConnectionValidates() {
        FakeDataSource dataSource = FakeDataSource.healthy();

        DatabaseHealth health = checkerFor(dataSource).check();

        assertThat(health.status()).isEqualTo(DatabaseHealth.Status.UP);
        assertThat(health.latencyMs()).isNotNull().isNotNegative();
        assertThat(health.error()).isNull();
        assertThat(dataSource.openConnections()).as("connection returned to the pool").isZero();
    }

    @Test
    void upReportsTheAppliedSchemaVersion() {
        FakeDataSource dataSource = FakeDataSource.migratedTo("2");

        DatabaseHealth health = checkerFor(dataSource).check();

        assertThat(health.status()).isEqualTo(DatabaseHealth.Status.UP);
        assertThat(health.schemaVersion()).isEqualTo("2");
        assertThat(dataSource.openConnections()).as("connection returned to the pool").isZero();
    }

    @Test
    void aReachableDatabaseThatWasNeverMigratedIsUpWithNoSchemaVersion() {
        DatabaseHealth health = checkerFor(FakeDataSource.healthy()).check();

        assertThat(health.status()).isEqualTo(DatabaseHealth.Status.UP);
        assertThat(health.schemaVersion()).isNull();
        assertThat(health.error()).isNull();
    }

    @Test
    void downWhenTheConnectionDoesNotValidate() {
        FakeDataSource dataSource = FakeDataSource.invalidConnection();

        DatabaseHealth health = checkerFor(dataSource).check();

        assertThat(health).isEqualTo(new DatabaseHealth(DatabaseHealth.Status.DOWN, null, "Connection validation failed", null));
        assertThat(dataSource.openConnections()).as("connection returned to the pool").isZero();
    }

    @Test
    void downReportsOnlyTheSqlStateNotTheDriverMessage() {
        // Driver messages can carry the host, database, user and even a password from the URL.
        SQLException failure = new SQLException(
                "FATAL: password authentication failed for user \"neondb_owner\" "
                        + "at ep-cool-darkness-123456-pooler.eu-central-1.aws.neon.tech password=s3cr3t",
                "28P01");

        DatabaseHealth health = checkerFor(FakeDataSource.failing(failure)).check();

        assertThat(health.status()).isEqualTo(DatabaseHealth.Status.DOWN);
        assertThat(health.latencyMs()).isNull();
        assertThat(health.error())
                .isEqualTo("Connection failed (SQLState 28P01)")
                .doesNotContain("s3cr3t", "neondb_owner", "neon.tech");
    }

    @Test
    void downWithAGenericErrorWhenTheDriverGivesNoSqlState() {
        DatabaseHealth health = checkerFor(FakeDataSource.failing(new SQLException("host unreachable"))).check();

        assertThat(health).isEqualTo(new DatabaseHealth(DatabaseHealth.Status.DOWN, null, "Connection failed", null));
    }
}
