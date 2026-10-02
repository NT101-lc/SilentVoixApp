package com.silentvoix.backend.database;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** The versioned migration scripts this build ships, in {@code db/migration}. */
public final class MigrationScripts {

    static final String LOCATION = "classpath:db/migration";

    /** Flyway's versioned naming: {@code V<version>__<description>.sql}, version parts split by dots. */
    private static final Pattern VERSIONED = Pattern.compile("V(\\d+(?:\\.\\d+)*)__.+\\.sql");

    private static final Comparator<String> BY_VERSION = (a, b) -> {
        List<BigInteger> left = parts(a);
        List<BigInteger> right = parts(b);
        for (int i = 0; i < Math.max(left.size(), right.size()); i++) {
            BigInteger l = i < left.size() ? left.get(i) : BigInteger.ZERO;
            BigInteger r = i < right.size() ? right.get(i) : BigInteger.ZERO;
            int compared = l.compareTo(r);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    };

    private static volatile String latest;

    private MigrationScripts() {
    }

    /** The version the database should be at once every bundled script has run. */
    public static String latestVersion() {
        String cached = latest;
        if (cached == null) {
            cached = latestOf(bundledFileNames());
            latest = cached;
        }
        return cached;
    }

    /** The highest version among [fileNames], or null when none is a versioned migration. */
    static String latestOf(Collection<String> fileNames) {
        return fileNames.stream()
                .map(VERSIONED::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .max(BY_VERSION)
                .orElse(null);
    }

    private static List<BigInteger> parts(String version) {
        return Arrays.stream(version.split("\\.")).map(BigInteger::new).toList();
    }

    private static List<String> bundledFileNames() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION + "/*.sql");
            return Arrays.stream(resources).map(Resource::getFilename).filter(Objects::nonNull).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list the migration scripts", e);
        }
    }
}
