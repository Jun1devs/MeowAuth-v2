package org.jun1devs.meowauth.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.security.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Менеджер данных пользователей.
 * Потокобезопасный, с асинхронным сохранением.
 */
public class UserDataManager {

    private UserDataManager() {
        // Utility class, no instances
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MeowAuth-SaveThread");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, UserEntry> users = new ConcurrentHashMap<>();
    private static Path dataFile = Path.of("config/meowauth_users.json");

    /** Установить путь к файлу данных и загрузить. */
    public static void setDataFile(Path path) {
        if (path != null) dataFile = path;
        load();
    }

    /** Зарегистрировать нового пользователя (генерирует токен). */
    public static String registerNewUser(String username, int tokenLengthBytes) {
        String token = generateToken(tokenLengthBytes);
        String hash = HashUtil.hash(token);
        long expiresAt = ConfigManager.getTokenExpirySeconds() > 0
                ? System.currentTimeMillis() + ConfigManager.getTokenExpirySeconds() * 1000
                : 0;
        UserEntry entry = new UserEntry(username, hash, System.currentTimeMillis(), expiresAt);
        users.put(username, entry);
        saveAsync();
        LOGGER.info("Registered new user '{}' (token expires: {})", username,
                expiresAt > 0 ? new java.util.Date(expiresAt) : "never");
        return token;
    }

    /** Обновить токен существующего пользователя (админский reset). */
    public static String refreshToken(String username, int tokenLengthBytes) {
        UserEntry existing = users.get(username);
        if (existing == null) {
            LOGGER.warn("Cannot refresh token for unregistered user '{}'", username);
            return null;
        }
        String token = generateToken(tokenLengthBytes);
        existing.tokenHash = HashUtil.hash(token);
        existing.registeredAt = System.currentTimeMillis();
        existing.tokenExpiresAt = ConfigManager.getTokenExpirySeconds() > 0
                ? System.currentTimeMillis() + ConfigManager.getTokenExpirySeconds() * 1000
                : 0;
        saveAsync();
        LOGGER.info("Refreshed token for user '{}'", username);
        return token;
    }

    /**
     * Rotate token after successful authentication.
     * Generates a new token, replaces the old hash, and saves immediately.
     * The old token becomes invalid — prevents replay attacks.
     */
    public static String rotateToken(String username, int tokenLengthBytes) {
        UserEntry existing = users.get(username);
        if (existing == null) {
            LOGGER.warn("Cannot rotate token for unregistered user '{}'", username);
            return null;
        }
        String newToken = generateToken(tokenLengthBytes);
        existing.tokenHash = HashUtil.hash(newToken);
        existing.registeredAt = System.currentTimeMillis();
        existing.tokenExpiresAt = ConfigManager.getTokenExpirySeconds() > 0
                ? System.currentTimeMillis() + ConfigManager.getTokenExpirySeconds() * 1000
                : 0;
        // Synchronous save — token rotation must persist before returning
        save();
        LOGGER.info("Rotated token for user '{}'", username);
        return newToken;
    }

    /** Проверить токен пользователя. */
    public static boolean verifyToken(String username, String token) {
        UserEntry entry = users.get(username);
        if (entry == null) {
            LOGGER.warn("User '{}' not found", username);
            return false;
        }
        // Check token expiry
        if (entry.tokenExpiresAt > 0 && System.currentTimeMillis() > entry.tokenExpiresAt) {
            LOGGER.warn("Token expired for user '{}'", username);
            return false;
        }
        boolean valid = HashUtil.verify(token, entry.tokenHash);
        if (!valid) {
            LOGGER.warn("Invalid token for user '{}'", username);
        }
        return valid;
    }

    /** Удалить пользователя. */
    public static boolean removeUser(String username) {
        boolean removed = users.remove(username) != null;
        if (removed) {
            saveAsync();
            LOGGER.info("Removed user '{}'", username);
        }
        return removed;
    }

    /** Проверить, зарегистрирован ли пользователь. */
    public static boolean isRegistered(String username) {
        return users.containsKey(username);
    }

    /** Atomically get or register a user (race condition protection). */
    public static String getOrRegisterUser(String username, int tokenLengthBytes) {
        // First try fast path — check if already exists
        if (users.containsKey(username)) {
            return null;
        }
        // Build entry outside of any lock
        String token = generateToken(tokenLengthBytes);
        String hash = HashUtil.hash(token);
        long expiresAt = ConfigManager.getTokenExpirySeconds() > 0
                ? System.currentTimeMillis() + ConfigManager.getTokenExpirySeconds() * 1000
                : 0;
        UserEntry entry = new UserEntry(username, hash, System.currentTimeMillis(), expiresAt);

        // Atomic insert — only if absent
        UserEntry existing = users.putIfAbsent(username, entry);
        if (existing == null) {
            // We were first — save synchronously to guarantee persistence
            save();
            LOGGER.info("Race-safe registered new user '{}' (token expires: {})", username,
                    expiresAt > 0 ? new java.util.Date(expiresAt) : "never");
            return token;
        }
        // Lost the race — another thread registered first
        return null;
    }

    /** Асинхронное сохранение. */
    private static void saveAsync() {
        if (ConfigManager.isAutoSave() && !SAVE_EXECUTOR.isShutdown()) {
            SAVE_EXECUTOR.submit(UserDataManager::save);
        }
    }

    /** Полностью сбросить состояние (для тестов). */
    public static void reset() {
        users.clear();
        dataFile = Path.of("config/meowauth_users.json");
        // SAVE_EXECUTOR is a daemon thread — won't stop between tests.
        // If shutdown was called, tests won't be able to save — expected behavior.
    }

    /** Синхронное сохранение на диск. */
    private static synchronized void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                GSON.toJson(users, writer);
            }
            Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Saved {} users to {}", users.size(), dataFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save user data", e);
        }
    }

    /** Загрузка из файла. */
    private static synchronized void load() {
        try {
            if (!Files.exists(dataFile)) {
                LOGGER.info("User data file not found, starting fresh: {}", dataFile);
                return;
            }
            try (Reader reader = Files.newBufferedReader(dataFile)) {
                Map<String, UserEntry> loaded = GSON.fromJson(reader, UserEntry.MAP_TYPE);
                if (loaded != null) {
                    users.clear();
                    users.putAll(loaded);
                    LOGGER.info("Loaded {} users from {}", users.size(), dataFile);
                }
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("Corrupted user data file: {}", dataFile, e);
        } catch (IOException e) {
            LOGGER.error("Failed to load user data from {}", dataFile, e);
        }
    }

    /** Завершить сохранение и остановить executor. */
    public static void shutdown() {
        save(); // Save first before shutdown
        SAVE_EXECUTOR.shutdown();
        try {
            if (!SAVE_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Save executor did not terminate within timeout, forcing shutdown");
                SAVE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SAVE_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for save executor", e);
        }
    }

    /** Запись пользователя. */
    private static class UserEntry {
        static final com.google.gson.reflect.TypeToken<Map<String, UserEntry>> MAP_TYPE =
                new com.google.gson.reflect.TypeToken<>() {};

        String username;
        String tokenHash;
        long registeredAt;
        long tokenExpiresAt;

        @SuppressWarnings("unused")
        UserEntry() {}

        UserEntry(String username, String tokenHash, long registeredAt, long tokenExpiresAt) {
            this.username = username;
            this.tokenHash = tokenHash;
            this.registeredAt = registeredAt;
            this.tokenExpiresAt = tokenExpiresAt;
        }
    }

    /** Генерация криптографически безопасного токена. */
    private static String generateToken(int lengthBytes) {
        int len = Math.max(8, Math.min(lengthBytes, 64));
        byte[] bytes = new byte[len];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
