package com.silentvoix.backend.database;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * PostgreSQL (Neon) connection settings, bound from {@code silentvoix.database.*}.
 * The values come from environment variables; see {@code application.properties} and {@code .env.example}.
 */
@ConfigurationProperties(prefix = "silentvoix.database")
public record DatabaseProperties(
        String url,
        String username,
        String password,
        @DefaultValue("5") int maximumPoolSize,
        @DefaultValue("10s") Duration connectionTimeout) {

    public boolean isConfigured() {
        return StringUtils.hasText(url);
    }

    // Never print the URL or password: either may carry credentials.
    @Override
    public String toString() {
        return "DatabaseProperties[url=" + (isConfigured() ? "<set>" : "<unset>")
                + ", username=" + username
                + ", password=<redacted>"
                + ", maximumPoolSize=" + maximumPoolSize
                + ", connectionTimeout=" + connectionTimeout + "]";
    }
}
