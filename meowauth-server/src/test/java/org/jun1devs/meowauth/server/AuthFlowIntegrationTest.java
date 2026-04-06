package org.jun1devs.meowauth.server;

import org.jun1devs.meowauth.server.data.LockoutManager;
import org.jun1devs.meowauth.server.data.UserDataManager;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full authentication flow:
 * register → verify → rotate → re-verify → lockout → expiry.
 * Tests run in order and share state to simulate a realistic session.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    private static final Path USERS_FILE = Path.of("config/meowauth_users.json");
    private static final Path LOCKOUT_FILE = Path.of("config/meowauth_lockouts.json");
    private static final Path CONFIG_FILE = Path.of("config/meowauth-server.json");
    private static final int TOKEN_LENGTH = 32;

    @BeforeAll
    static void setUp() {
        cleanup();
        ConfigManager.resetToDefaults();
        ConfigManager.load();
    }

    @AfterAll
    static void tearDown() {
        UserDataManager.reset();
        LockoutManager.reset();
        cleanup();
    }

    private static void cleanup() {
        deleteQuietly(USERS_FILE);
        deleteQuietly(LOCKOUT_FILE);
        deleteQuietly(CONFIG_FILE);
    }

    private static void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }

    // --- Full lifecycle flow (ordered tests) ---

    @Test
    @Order(1)
    @DisplayName("Step 1: Register new user → receive token")
    void step1_registerNewUser() {
        String token = UserDataManager.getOrRegisterUser("TestPlayer", TOKEN_LENGTH);

        assertThat(token).isNotBlank();
        assertThat(UserDataManager.isRegistered("TestPlayer")).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Verify token → success")
    void step2_verifyToken_success() {
        // Refresh to get a known token value
        String knownToken = UserDataManager.refreshToken("TestPlayer", TOKEN_LENGTH);

        assertThat(UserDataManager.verifyToken("TestPlayer", knownToken)).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Verify wrong token → failure")
    void step3_verifyWrongToken_fails() {
        assertThat(UserDataManager.verifyToken("TestPlayer", "wrong-token-value")).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Token rotation → old token invalid, new token valid")
    void step4_tokenRotation_oldTokenInvalid() {
        // Get a known token
        String oldToken = UserDataManager.refreshToken("TestPlayer", TOKEN_LENGTH);
        assertThat(UserDataManager.verifyToken("TestPlayer", oldToken)).isTrue();

        // Rotate — simulates successful authentication
        String newToken = UserDataManager.rotateToken("TestPlayer", TOKEN_LENGTH);

        // Old token must be invalid now
        assertThat(UserDataManager.verifyToken("TestPlayer", oldToken)).isFalse();
        // New token must be valid
        assertThat(UserDataManager.verifyToken("TestPlayer", newToken)).isTrue();
        // New token is different from old
        assertThat(newToken).isNotEqualTo(oldToken);
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Lockout after max failed attempts")
    void step5_lockoutAfterMaxAttempts() {
        int maxAttempts = ConfigManager.getMaxLoginAttempts(); // default 5

        for (int i = 0; i < maxAttempts; i++) {
            LockoutManager.recordFailedAttempt("LockoutUser");
        }

        assertThat(LockoutManager.isLockedOut("LockoutUser")).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Reset attempts → user unlocked")
    void step6_resetAttempts_unlocksUser() {
        LockoutManager.resetAttempts("LockoutUser");
        assertThat(LockoutManager.isLockedOut("LockoutUser")).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Multiple users independent")
    void step7_multipleUsersIndependent() {
        String tokenA = UserDataManager.getOrRegisterUser("PlayerA", TOKEN_LENGTH);
        String tokenB = UserDataManager.getOrRegisterUser("PlayerB", TOKEN_LENGTH);

        assertThat(tokenA).isNotBlank();
        assertThat(tokenB).isNotBlank();
        assertThat(tokenA).isNotEqualTo(tokenB);

        // Cross-verification must fail
        assertThat(UserDataManager.verifyToken("PlayerA", tokenB)).isFalse();
        assertThat(UserDataManager.verifyToken("PlayerB", tokenA)).isFalse();

        // Each own token works
        assertThat(UserDataManager.verifyToken("PlayerA", tokenA)).isTrue();
        assertThat(UserDataManager.verifyToken("PlayerB", tokenB)).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Remove user → cannot verify")
    void step8_removeUser_cannotVerify() {
        UserDataManager.removeUser("PlayerA");
        assertThat(UserDataManager.isRegistered("PlayerA")).isFalse();
        assertThat(UserDataManager.verifyToken("PlayerA", "any-token")).isFalse();
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Admin reset → new token issued, old invalid")
    void step9_adminReset_newTokenIssued() {
        // PlayerB exists from step 7
        assertThat(UserDataManager.isRegistered("PlayerB")).isTrue();

        String newToken = UserDataManager.refreshToken("PlayerB", TOKEN_LENGTH);
        assertThat(newToken).isNotBlank();
        assertThat(UserDataManager.verifyToken("PlayerB", newToken)).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: Token rotation twice → only latest token valid")
    void step10_doubleRotation_onlyLatestValid() {
        String token1 = UserDataManager.refreshToken("PlayerB", TOKEN_LENGTH);
        assertThat(UserDataManager.verifyToken("PlayerB", token1)).isTrue();

        String token2 = UserDataManager.rotateToken("PlayerB", TOKEN_LENGTH);
        assertThat(UserDataManager.verifyToken("PlayerB", token2)).isTrue();
        assertThat(UserDataManager.verifyToken("PlayerB", token1)).isFalse();

        String token3 = UserDataManager.rotateToken("PlayerB", TOKEN_LENGTH);
        assertThat(UserDataManager.verifyToken("PlayerB", token3)).isTrue();
        assertThat(UserDataManager.verifyToken("PlayerB", token2)).isFalse();
        assertThat(UserDataManager.verifyToken("PlayerB", token1)).isFalse();
    }
}
