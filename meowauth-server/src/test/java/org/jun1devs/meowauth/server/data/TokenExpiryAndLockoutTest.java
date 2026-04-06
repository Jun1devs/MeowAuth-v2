package org.jun1devs.meowauth.server.data;

import org.jun1devs.meowauth.server.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты истечения токенов и блокировок.
 */
class TokenExpiryAndLockoutTest {

    private static final Path USERS_FILE = Path.of("config/meowauth_users.json");
    private static final Path LOCKOUT_FILE = Path.of("config/meowauth_lockouts.json");

    @BeforeEach
    void setUp() {
        ConfigManager.resetToDefaults();
        ConfigManager.load();
        deleteQuietly(USERS_FILE);
        deleteQuietly(LOCKOUT_FILE);
    }

    @AfterEach
    void tearDown() {
        UserDataManager.reset();
        LockoutManager.reset();
        deleteQuietly(USERS_FILE);
        deleteQuietly(LOCKOUT_FILE);
    }

    private static void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }

    @Test
    void verifyToken_expiredToken_returnsFalse() throws IOException {
        // Устанавливаем токен с истечением 1 секунда
        // Записываем файл вручную с прошлым временем
        String json = """
                {
                  "ExpiredUser": {
                    "username": "ExpiredUser",
                    "tokenHash": "$2a$12$dummyhashvaluethatis60characterslongxxxxxxxxxxxxx",
                    "registeredAt": 1000000,
                    "tokenExpiresAt": 2000000
                  }
                }
                """;
        Files.createDirectories(USERS_FILE.getParent());
        Files.writeString(USERS_FILE, json);

        UserDataManager.setDataFile(USERS_FILE);

        // tokenExpiresAt = 2000000 (1970 год), сейчас >> 2000000
        assertThat(UserDataManager.verifyToken("ExpiredUser", "anyToken")).isFalse();
    }

    @Test
    void lockout_expiryAfterDuration_returnsFalse() throws IOException {
        // Записываем lockout с прошлым временем (1 час назад)
        long oneHourAgo = (System.currentTimeMillis() / 1000) - 3600;
        String json = """
                {
                  "LockedUser": {
                    "count": 5,
                    "lastAttemptEpoch": %d
                  }
                }
                """.formatted(oneHourAgo);
        Files.createDirectories(LOCKOUT_FILE.getParent());
        Files.writeString(LOCKOUT_FILE, json);

        LockoutManager.load();

        // lockoutDurationSeconds = 300 (5 мин), прошло 3600 сек — истёк
        assertThat(LockoutManager.isLockedOut("LockedUser")).isFalse();
    }
}
