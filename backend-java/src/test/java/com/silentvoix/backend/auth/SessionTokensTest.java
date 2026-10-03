package com.silentvoix.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SessionTokensTest {

    @Test
    void aTokenIsUrlSafeAndCarries256RandomBits() {
        String token = SessionTokens.newToken();

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void tokensDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(seen.add(SessionTokens.newToken())).isTrue();
        }
    }

    @Test
    void theStoredFormIsA32ByteSha256() {
        byte[] hash = SessionTokens.hash("abc");

        assertThat(hash).hasSize(32);
        // SHA-256("abc"), so the database can be checked by hand.
        assertThat(java.util.HexFormat.of().formatHex(hash))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
