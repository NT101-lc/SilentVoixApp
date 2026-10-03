package com.silentvoix.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void aHashIsBcryptAndNeverThePasswordItself() {
        String hash = hasher.hash("mật khẩu dài");

        assertThat(hash).startsWith("$2").hasSize(60).doesNotContain("mật khẩu dài");
    }

    @Test
    void theSamePasswordMatchesAndAnotherDoesNot() {
        String hash = hasher.hash("correct horse");

        assertThat(hasher.matches("correct horse", hash)).isTrue();
        assertThat(hasher.matches("correct hors", hash)).isFalse();
    }

    @Test
    void hashingTwiceGivesDifferentSalts() {
        assertThat(hasher.hash("same")).isNotEqualTo(hasher.hash("same"));
    }

    @Test
    void aMissingOrBrokenHashNeverMatches() {
        assertThat(hasher.matches("anything", null)).isFalse();
        assertThat(hasher.matches("anything", "not-a-hash")).isFalse();
    }
}
