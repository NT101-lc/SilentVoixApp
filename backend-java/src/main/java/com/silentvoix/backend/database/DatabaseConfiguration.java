package com.silentvoix.backend.database;

import java.time.Duration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Creates the PostgreSQL pool only when {@code SILENTVOIX_DATABASE_URL} is set, so the backend
 * still starts (and reports {@code NOT_CONFIGURED}) without database settings.
 */
@Configuration(proxyBeanMethods = false)
class DatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    private static final String JDBC_PREFIX = "jdbc:postgresql://";

    @Bean(destroyMethod = "close")
    @Conditional(DatabaseUrlPresent.class)
    HikariDataSource dataSource(DatabaseProperties properties) {
        String url = properties.url().trim();
        if (!url.startsWith(JDBC_PREFIX)) {
            // Do not echo the value: Neon connection strings embed the password.
            throw new IllegalStateException("SILENTVOIX_DATABASE_URL must be a JDBC URL starting with "
                    + JDBC_PREFIX + " (Neon shows postgresql://...; see backend-java/.env.example)");
        }
        if (url.contains(".neon.tech") && !url.contains("-pooler.")) {
            log.warn("SILENTVOIX_DATABASE_URL uses a direct Neon endpoint; prefer the pooled (-pooler) endpoint");
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("silentvoix-db");
        config.setJdbcUrl(url);
        if (StringUtils.hasText(properties.username())) {
            config.setUsername(properties.username());
        }
        if (StringUtils.hasText(properties.password())) {
            config.setPassword(properties.password());
        }
        config.setMaximumPoolSize(properties.maximumPoolSize());
        config.setConnectionTimeout(properties.connectionTimeout().toMillis());
        // Hold no idle connections for long, so Neon can scale the compute to zero.
        config.setMinimumIdle(0);
        config.setIdleTimeout(Duration.ofMinutes(1).toMillis());
        // Start even if Neon is unreachable; the health endpoint reports the failure instead.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    static class DatabaseUrlPresent implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return StringUtils.hasText(context.getEnvironment().getProperty("silentvoix.database.url"));
        }
    }
}
