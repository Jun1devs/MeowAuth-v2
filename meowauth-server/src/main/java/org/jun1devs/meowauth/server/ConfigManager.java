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

    /** Публичные настраиваемые параметры */
    public static boolean debug = false;
    public static int tokenLength = 32;
    public static String dataFile = "config/meowauth_users.json";
    public static boolean autoSave = true;
    public static String kickMessage = "§cAuthentication failed: invalid or missing token.";
    public static int maxLoginAttempts = 5;
    public static long lockoutDurationSeconds = 300;

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
                    tokenLength = clamp(data.tokenLength, 8, 64);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
