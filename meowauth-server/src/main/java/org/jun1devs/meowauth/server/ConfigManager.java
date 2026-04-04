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

    public static boolean isDebug() { return debug; }
    public static int getTokenLength() { return tokenLength; }
    public static String getDataFile() { return dataFile; }
    public static String getKickMessage() { return kickMessage; }
    public static int getMaxLoginAttempts() { return maxLoginAttempts; }
    public static long getLockoutDurationSeconds() { return lockoutDurationSeconds; }

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
                    dataFile = coalesce(data.dataFile, dataFile);
                    autoSave = data.autoSave;
                    kickMessage = coalesce(data.kickMessage, kickMessage);
                    maxLoginAttempts = Math.max(1, data.maxLoginAttempts);
                    lockoutDurationSeconds = Math.max(0, data.lockoutDurationSeconds);
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

    private static class ConfigData {
        boolean debug = ConfigManager.debug;
        int tokenLength = ConfigManager.tokenLength;
        String dataFile = ConfigManager.dataFile;
        boolean autoSave = ConfigManager.autoSave;
        String kickMessage = ConfigManager.kickMessage;
        int maxLoginAttempts = ConfigManager.maxLoginAttempts;
        long lockoutDurationSeconds = ConfigManager.lockoutDurationSeconds;

        ConfigData() {}
    }
}
