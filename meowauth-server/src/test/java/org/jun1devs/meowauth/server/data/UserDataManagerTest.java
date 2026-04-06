package org.jun1devs.meowauth.server.data;

import org.jun1devs.meowauth.server.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link UserDataManager}.
 */
class UserDataManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ConfigManager.load();
        // Чистим перед каждым тестом
        LockoutManager.reset();
        // Удаляем файлы данных если остались
        try { Files.deleteIfExists(Path.of("config/meowauth_users.json")); } catch (IOException ignored) {}
        try { Files.deleteIfExists(Path.of("config/meowauth_users.json.tmp")); } catch (IOException ignored) {}
    }

    @AfterEach
    void tearDown() {
        UserDataManager.reset();
        LockoutManager.reset();
        try { Files.deleteIfExists(Path.of("config/meowauth_users.json")); } catch (IOException ignored) {}
    }

    @Test
    void registerNewUser_generatesToken() {
        String token = UserDataManager.registerNewUser("Alice", 32);

        assertThat(token).isNotBlank();
        assertThat(UserDataManager.isRegistered("Alice")).isTrue();
    }

    @Test
    void verifyToken_validToken_returnsTrue() {
        String token = UserDataManager.registerNewUser("Bob", 32);

        assertThat(UserDataManager.verifyToken("Bob", token)).isTrue();
    }

    @Test
    void verifyToken_wrongToken_returnsFalse() {
        UserDataManager.registerNewUser("Charlie", 32);

        assertThat(UserDataManager.verifyToken("Charlie", "wrongToken123")).isFalse();
    }

    @Test
    void verifyToken_unknownUser_returnsFalse() {
        assertThat(UserDataManager.verifyToken("Nobody", "anyToken")).isFalse();
    }

    @Test
    void removeUser_removesFromStore() {
        UserDataManager.registerNewUser("Dave", 32);
        assertThat(UserDataManager.isRegistered("Dave")).isTrue();

        boolean removed = UserDataManager.removeUser("Dave");

        assertThat(removed).isTrue();
        assertThat(UserDataManager.isRegistered("Dave")).isFalse();
    }

    @Test
    void removeUser_nonExistent_returnsFalse() {
        assertThat(UserDataManager.removeUser("Nobody")).isFalse();
    }

    @Test
    void refreshToken_generatesNewToken() {
        String oldToken = UserDataManager.registerNewUser("Eve", 32);
        assertThat(UserDataManager.verifyToken("Eve", oldToken)).isTrue();

        String newToken = UserDataManager.refreshToken("Eve", 32);

        assertThat(newToken).isNotBlank();
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(UserDataManager.verifyToken("Eve", newToken)).isTrue();
        // Старый токен больше не валиден
        assertThat(UserDataManager.verifyToken("Eve", oldToken)).isFalse();
    }

    @Test
    void refreshToken_nonExistentUser_returnsNull() {
        assertThat(UserDataManager.refreshToken("Nobody", 32)).isNull();
    }

    @Test
    void setDataFile_loadsExistingData() throws IOException {
        // Создаём файл вручную
        String json = """
                {
                  "OldUser": {
                    "username": "OldUser",
                    "tokenHash": "$2a$12$dummyhashvaluethatis60characterslongxxxxxxxxxxxxx",
                    "registeredAt": 1000000,
                    "tokenExpiresAt": 0
                  }
                }
                """;
        Path customFile = tempDir.resolve("custom_users.json");
        Files.writeString(customFile, json);

        UserDataManager.setDataFile(customFile);

        assertThat(UserDataManager.isRegistered("OldUser")).isTrue();
    }

    @Test
    void registerNewUser_differentUsersIndependent() {
        String token1 = UserDataManager.registerNewUser("User1", 32);
        String token2 = UserDataManager.registerNewUser("User2", 32);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(UserDataManager.verifyToken("User1", token1)).isTrue();
        assertThat(UserDataManager.verifyToken("User2", token2)).isTrue();
        // Кросс-проверка — должен быть false
        assertThat(UserDataManager.verifyToken("User1", token2)).isFalse();
    }
}
