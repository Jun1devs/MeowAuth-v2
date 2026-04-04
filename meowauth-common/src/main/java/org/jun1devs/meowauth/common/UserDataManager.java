package org.jun1devs.meowauth.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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

    /** Зарегистрировать нового пользователя или вернуть существующий хеш. */
    public static String registerOrGetHash(String username, int tokenLengthBytes) {
        UserEntry existing = users.get(username);
        if (existing != null) {
            LOGGER.debug("User '{}' already registered, returning existing hash", username);
            return existing.tokenHash;
        }

        String token = generateToken(tokenLengthBytes);
        String hash = HashUtil.hash(token);
        UserEntry entry = new UserEntry(username, hash, System.currentTimeMillis());
        users.put(username, entry);
        saveAsync();
        LOGGER.info("Registered new user '{}'", username);
        return token;
    }

    /** Проверить токен пользователя. */
    public static boolean verifyToken(String username, String token) {
        UserEntry entry = users.get(username);
        if (entry == null) {
            LOGGER.warn("User '{}' not found", username);
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

    /** Количество зарегистрированных пользователей. */
    public int getUserCount() {
        return users.size();
    }

    /** Асинхронное сохранение. */
    private static void saveAsync() {
        SAVE_EXECUTOR.submit(UserDataManager::save);
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
        save();
        try {
            SAVE_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        SAVE_EXECUTOR.shutdownNow();
    }

    /** Запись пользователя. */
    private static class UserEntry {
        static final com.google.gson.reflect.TypeToken<Map<String, UserEntry>> MAP_TYPE =
                new com.google.gson.reflect.TypeToken<Map<String, UserEntry>>() {};

        String username;
        String tokenHash;
        long registeredAt;

        @SuppressWarnings("unused")
        UserEntry() {}

        UserEntry(String username, String tokenHash, long registeredAt) {
            this.username = username;
            this.tokenHash = tokenHash;
            this.registeredAt = registeredAt;
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
