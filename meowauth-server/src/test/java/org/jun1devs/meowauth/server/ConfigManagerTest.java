package org.jun1devs.meowauth.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link ConfigManager}.
 * Каждый тест изолирован — файл конфига удаляется до и после.
 */
class ConfigManagerTest {

    private static final Path CFG = Path.of("config/meowauth-server.json");

    @BeforeEach
    void setUp() throws IOException {
        deleteConfig();
        ConfigManager.resetToDefaults();
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteConfig();
    }

    private static void deleteConfig() throws IOException {
        try { Files.deleteIfExists(CFG); } catch (IOException ignored) {}
    }

    @Test
    void load_createsDefaultConfig() {
        ConfigManager.load();

        assertThat(ConfigManager.isDebug()).isFalse();
        assertThat(ConfigManager.getTokenLength()).isEqualTo(32);
        assertThat(ConfigManager.getDataFile()).isEqualTo("config/meowauth_users.json");
        assertThat(ConfigManager.getKickMessage()).isNotBlank();
        assertThat(ConfigManager.getMaxLoginAttempts()).isEqualTo(5);
        assertThat(ConfigManager.getLockoutDurationSeconds()).isEqualTo(300);
        assertThat(ConfigManager.getTokenExpirySeconds()).isEqualTo(0);
    }

    @Test
    void load_parsesCustomConfig() throws IOException {
        // Сначала дефолты
        ConfigManager.load();
        // Теперь кастомный конфиг
        Files.createDirectories(CFG.getParent());
        String json = """
                {
                  "debug": true,
                  "tokenLength": 16,
                  "dataFile": "custom/path/users.json",
                  "autoSave": false,
                  "kickMessage": "Custom kick",
                  "maxLoginAttempts": 3,
                  "lockoutDurationSeconds": 600,
                  "tokenExpirySeconds": 86400
                }
                """;
        Files.writeString(CFG, json);

        ConfigManager.load();

        assertThat(ConfigManager.isDebug()).isTrue();
        assertThat(ConfigManager.getTokenLength()).isEqualTo(16);
        assertThat(ConfigManager.getDataFile()).isEqualTo("custom/path/users.json");
        assertThat(ConfigManager.getKickMessage()).isEqualTo("Custom kick");
        assertThat(ConfigManager.getMaxLoginAttempts()).isEqualTo(3);
        assertThat(ConfigManager.getLockoutDurationSeconds()).isEqualTo(600);
        assertThat(ConfigManager.getTokenExpirySeconds()).isEqualTo(86400);
    }

    @Test
    void load_clampsTokenLengthToValidRange() throws IOException {
        // Сначала дефолты
        ConfigManager.load();
        // Теперь невалидный
        Files.createDirectories(CFG.getParent());
        Files.writeString(CFG, "{\"tokenLength\": 0}");

        ConfigManager.load();

        assertThat(ConfigManager.getTokenLength()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void save_createsValidJson() throws IOException {
        ConfigManager.load();
        ConfigManager.save();

        assertThat(Files.exists(CFG)).isTrue();
        String content = Files.readString(CFG);
        assertThat(content).contains("debug");
        assertThat(content).contains("tokenLength");
    }

    @Test
    void load_corruptedJson_preservesPreviousValues() throws IOException {
        // Загружаем дефолты
        ConfigManager.load();
        assertThat(ConfigManager.isDebug()).isFalse();

        // Пишем мусор
        Files.createDirectories(CFG.getParent());
        Files.writeString(CFG, "NOT VALID JSON {{{");

        // Загрузка мусора должна оставить предыдущие значения
        ConfigManager.load();

        assertThat(ConfigManager.isDebug()).isFalse();
        assertThat(ConfigManager.getTokenLength()).isEqualTo(32);
    }
}
