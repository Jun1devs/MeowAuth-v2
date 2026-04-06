package org.jun1devs.meowauth.server.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jun1devs.meowauth.server.ConfigManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link LockoutManager}.
 * Тесты используют реальную файловую систему (временная директория).
 */
class LockoutManagerTest {

    private Path tempDir;
    private Path originalLockoutFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("meowauth-lockout-test");
        LockoutManager.reset();
        ConfigManager.load();
    }

    @AfterEach
    void tearDown() throws Exception {
        LockoutManager.reset();
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                    });
        }
    }

    @Test
    void recordFailedAttempt_marksAsLockedAfterThreshold() {
        int maxAttempts = ConfigManager.getMaxLoginAttempts(); // 5 by default

        for (int i = 0; i < maxAttempts; i++) {
            LockoutManager.recordFailedAttempt("testuser");
        }

        assertThat(LockoutManager.isLockedOut("testuser")).isTrue();
    }

    @Test
    void resetAttempts_clearsLockout() {
        int maxAttempts = ConfigManager.getMaxLoginAttempts();

        for (int i = 0; i < maxAttempts; i++) {
            LockoutManager.recordFailedAttempt("testuser");
        }
        assertThat(LockoutManager.isLockedOut("testuser")).isTrue();

        LockoutManager.resetAttempts("testuser");
        assertThat(LockoutManager.isLockedOut("testuser")).isFalse();
    }

    @Test
    void isLockedOut_unknownUser_returnsFalse() {
        assertThat(LockoutManager.isLockedOut("nonexistent")).isFalse();
    }

    @Test
    void isLockedOut_belowThreshold_returnsFalse() {
        int maxAttempts = ConfigManager.getMaxLoginAttempts();

        // На 1 меньше порога
        for (int i = 0; i < maxAttempts - 1; i++) {
            LockoutManager.recordFailedAttempt("testuser");
        }

        assertThat(LockoutManager.isLockedOut("testuser")).isFalse();
    }

    @Test
    void shutdown_savesAndTerminates() throws Exception {
        // Record some data
        LockoutManager.recordFailedAttempt("shutdownuser");
        // Shutdown should save and terminate gracefully
        LockoutManager.shutdown();
        // After shutdown, the file should exist
        assertThat(Files.exists(Path.of("config/meowauth_lockouts.json"))).isTrue();
    }
}
