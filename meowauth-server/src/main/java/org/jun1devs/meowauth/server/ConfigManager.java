package org.jun1devs.meowauth.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Менеджер конфигурации сервера.
 * Загружает/сохраняет JSON-конфиг, использует безопасные значения по умолчанию.
 */
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CFG = Path.of("config/meowauth-server.json");

    private ConfigManager() {
        // Utility class, no instances
    }

    /** Публичные настраиваемые параметры */
    private static boolean debug = false;
    private static int tokenLength = 32;
    private static String dataFile = "config/meowauth_users.json";
    private static boolean autoSave = true;
    private static String kickMessage = "§cAuthentication failed: invalid or missing token.";
    private static int maxLoginAttempts = 5;
    private static long lockoutDurationSeconds = 300;
    private static long tokenExpirySeconds = 0; // 0 = never expires

    public static boolean isDebug() { return debug; }
    public static int getTokenLength() { return tokenLength; }
    public static String getDataFile() { return dataFile; }
    public static String getKickMessage() { return kickMessage; }
    public static int getMaxLoginAttempts() { return maxLoginAttempts; }
    public static long getLockoutDurationSeconds() { return lockoutDurationSeconds; }
    public static long getTokenExpirySeconds() { return tokenExpirySeconds; }
    public static boolean isAutoSave() { return autoSave; }

    public static void load() {
        try {
            if (!Files.exists(CFG)) {
                LOGGER.info("Config not found, creating default: {}", CFG);
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(CFG)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    debug = data.debug;
                    tokenLength = Math.max(1, Math.min(data.tokenLength, 64));
                    dataFile = validateDataFile(data.dataFile, dataFile);
                    autoSave = data.autoSave;
                    kickMessage = coalesce(data.kickMessage, kickMessage);
                    maxLoginAttempts = Math.max(1, data.maxLoginAttempts);
                    lockoutDurationSeconds = Math.max(0, data.lockoutDurationSeconds);
                    tokenExpirySeconds = Math.max(0, data.tokenExpirySeconds);
                }
            }
            LOGGER.info("Config loaded from {}", CFG);
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CFG.getParent());
            try (Writer writer = Files.newBufferedWriter(CFG)) {
                GSON.toJson(new ConfigData(), writer);
            }
            LOGGER.debug("Config saved to {}", CFG);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static String coalesce(String value, String fallback) {
        return value != null ? value : fallback;
    }

    /** Валидация dataFile — защита от path traversal. */
    private static String validateDataFile(String value, String fallback) {
        String path = coalesce(value, fallback);
        Path resolved = Paths.get(path).normalize();
        String normalized = resolved.toString();
        // Check for path traversal — reject paths escaping server directory
        if (normalized.startsWith("..") || normalized.contains("/../") || normalized.contains("\\..\\")) {
            LOGGER.warn("Path traversal attempt in dataFile: '{}', using fallback: '{}'", value, fallback);
            return fallback;
        }
        return path;
    }

    /** Сбросить все поля к дефолтным значениям (для тестов). */
    public static void resetToDefaults() {
        debug = false;
        tokenLength = 32;
        dataFile = "config/meowauth_users.json";
        autoSave = true;
        kickMessage = "§cAuthentication failed: invalid or missing token.";
        maxLoginAttempts = 5;
        lockoutDurationSeconds = 300;
        tokenExpirySeconds = 0;
    }

    private static class ConfigData {
        boolean debug = ConfigManager.debug;
        int tokenLength = ConfigManager.tokenLength;
        String dataFile = ConfigManager.dataFile;
        boolean autoSave = ConfigManager.autoSave;
        String kickMessage = ConfigManager.kickMessage;
        int maxLoginAttempts = ConfigManager.maxLoginAttempts;
        long lockoutDurationSeconds = ConfigManager.lockoutDurationSeconds;
        long tokenExpirySeconds = ConfigManager.tokenExpirySeconds;

        ConfigData() {}
    }
}
