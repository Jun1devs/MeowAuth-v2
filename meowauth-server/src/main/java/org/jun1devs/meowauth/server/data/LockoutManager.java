package org.jun1devs.meowauth.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jun1devs.meowauth.server.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Менеджер блокировок — сохраняет неудачные попытки входа на диск,
 * чтобы блокировка переживала перезапуск сервера.
 */
public class LockoutManager {

    private LockoutManager() {
        // Utility class, no instances
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LockoutManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LOCKOUT_FILE = Path.of("config/meowauth_lockouts.json");

    private static final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MeowAuth-LockoutSaveThread");
        t.setDaemon(true);
        return t;
    });

    /** Загрузить блокировки из файла. */
    public static void load() {
        try {
            if (!Files.exists(LOCKOUT_FILE)) {
                LOGGER.debug("Lockout file not found, starting fresh: {}", LOCKOUT_FILE);
                return;
            }
            try (Reader reader = Files.newBufferedReader(LOCKOUT_FILE)) {
                Map<String, LoginAttempt> loaded = GSON.fromJson(reader, LoginAttempt.MAP_TYPE);
                if (loaded != null) {
                    attempts.clear();
                    attempts.putAll(loaded);
                    LOGGER.info("Loaded {} lockout entries from {}", attempts.size(), LOCKOUT_FILE);
                }
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("Corrupted lockout file: {}", LOCKOUT_FILE, e);
        } catch (IOException e) {
            LOGGER.error("Failed to load lockout data from {}", LOCKOUT_FILE, e);
        }
    }

    /** Проверить, заблокирован ли пользователь. */
    public static boolean isLockedOut(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null) return false;

        if (attempt.count >= ConfigManager.getMaxLoginAttempts()) {
            long elapsed = Instant.now().getEpochSecond() - attempt.lastAttemptEpoch;
            if (elapsed < ConfigManager.getLockoutDurationSeconds()) {
                LOGGER.warn("User '{}' is locked out ({} attempts, {}s remaining)",
                        username, attempt.count, ConfigManager.getLockoutDurationSeconds() - elapsed);
                return true;
            } else {
                attempts.remove(username);
                saveAsync(); // Defer to async save — avoid blocking the read path
                LOGGER.info("User '{}' lockout expired", username);
            }
        }
        return false;
    }

    /** Записать неудачную попытку входа. */
    public static void recordFailedAttempt(String username) {
        attempts.compute(username, (key, existing) -> {
            if (existing == null) {
                return new LoginAttempt(1, Instant.now().getEpochSecond());
            }
            return new LoginAttempt(existing.count + 1, Instant.now().getEpochSecond());
        });
        saveAsync();
    }

    /** Сбросить счётчик после успешного входа. */
    public static void resetAttempts(String username) {
        attempts.remove(username);
        saveAsync();
    }

    /** Асинхронное сохранение на диск. */
    private static void saveAsync() {
        if (!SAVE_EXECUTOR.isShutdown()) {
            SAVE_EXECUTOR.submit(LockoutManager::save);
        }
    }

    /** Синхронное сохранение на диск. */
    private static synchronized void save() {
        try {
            Files.createDirectories(LOCKOUT_FILE.getParent());
            Path tempFile = LOCKOUT_FILE.resolveSibling(LOCKOUT_FILE.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                GSON.toJson(attempts, writer);
            }
            Files.move(tempFile, LOCKOUT_FILE, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Saved {} lockout entries to {}", attempts.size(), LOCKOUT_FILE);
        } catch (IOException e) {
            LOGGER.error("Failed to save lockout data", e);
        }
    }

    /** Полностью сбросить состояние (для тестов). */
    static void reset() {
        attempts.clear();
    }

    /** Остановить executor и сохранить данные (вызывается при остановке сервера). */
    public static void shutdown() {
        save(); // Save first
        SAVE_EXECUTOR.shutdown();
        try {
            if (!SAVE_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
                LOGGER.warn("Lockout save executor did not terminate, forcing shutdown");
                SAVE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SAVE_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class LoginAttempt {
        static final com.google.gson.reflect.TypeToken<Map<String, LoginAttempt>> MAP_TYPE =
                new com.google.gson.reflect.TypeToken<>() {};

        int count;
        long lastAttemptEpoch;

        @SuppressWarnings("unused")
        LoginAttempt() {}

        LoginAttempt(int count, long lastAttemptEpoch) {
            this.count = count;
            this.lastAttemptEpoch = lastAttemptEpoch;
        }
    }
}
