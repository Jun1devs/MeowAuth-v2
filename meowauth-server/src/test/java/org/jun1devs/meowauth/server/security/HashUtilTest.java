package org.jun1devs.meowauth.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для {@link HashUtil}.
 */
class HashUtilTest {

    @Test
    void hashAndVerify_validToken_returnsTrue() {
        String token = "TestToken123";
        String hash = HashUtil.hash(token);

        assertNotNull(hash);
        assertEquals(60, hash.length());
        assertTrue(HashUtil.verify(token, hash));
    }

    @Test
    void verify_wrongToken_returnsFalse() {
        String hash = HashUtil.hash("CorrectToken1");

        assertFalse(HashUtil.verify("WrongToken", hash));
    }

    @Test
    void hash_sameTokenProducesDifferentHashes() {
        String token = "SameToken";
        String hash1 = HashUtil.hash(token);
        String hash2 = HashUtil.hash(token);

        // BCrypt генерирует разные хеши для одного и того же входа
        assertNotEquals(hash1, hash2);
        // Но оба верифицируются
        assertTrue(HashUtil.verify(token, hash1));
        assertTrue(HashUtil.verify(token, hash2));
    }

    @Test
    void hash_tooShortToken_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> HashUtil.hash("abc"));
    }

    @Test
    void hash_tooLongToken_throwsException() {
        String longToken = "A".repeat(73);
        assertThrows(IllegalArgumentException.class, () -> HashUtil.hash(longToken));
    }

    @Test
    void validate_boundaryMinLength_succeeds() {
        // validate is private, tested indirectly via hash()
        assertDoesNotThrow(() -> HashUtil.hash("abcdef")); // 6 символов
    }

    @Test
    void validate_boundaryMaxLength_succeeds() {
        String maxToken = "A".repeat(72);
        assertDoesNotThrow(() -> HashUtil.hash(maxToken));
    }

    @Test
    void verify_nullInputs_returnsFalse() {
        assertFalse(HashUtil.verify(null, "somehash"));
        assertFalse(HashUtil.verify("token", null));
    }
}
