package org.jun1devs.meowauth.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class TokenReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenReceiver.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path TOKEN_FILE = Path.of("config/meowauth-client.json");
    private static final String KEY_TOKEN = "token";

    /** Simple XOR encoding — prevents casual reading of the token file.
     *  WARNING: This is NOT encryption. Anyone with the mod JAR can reverse this.
     *  Its only purpose is to avoid storing tokens in plain text at a glance. */
    private static final byte[] ENCODING_KEY = "MeowAuth-2026-Token-Encoding".getBytes(StandardCharsets.UTF_8);

    private TokenReceiver() {
        // Utility class, no instances
    }

    private static String cachedToken = null;

    /** Save token to file (XOR-encoded). */
    public static void saveToken(String token) {
        if (token == null || token.isEmpty()) {
            LOGGER.warn("Attempted to save null/empty token");
            return;
        }
        try {
            Files.createDirectories(TOKEN_FILE.getParent());
            JsonObject json = new JsonObject();
            json.addProperty(KEY_TOKEN, encode(token));
            json.addProperty("savedAt", System.currentTimeMillis());
            // Атомарная запись через .tmp + move
            Path tempFile = TOKEN_FILE.resolveSibling(TOKEN_FILE.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                GSON.toJson(json, writer);
            }
            Files.move(tempFile, TOKEN_FILE, StandardCopyOption.REPLACE_EXISTING);
            restrictFilePermissions(TOKEN_FILE);
            cachedToken = token;
            LOGGER.debug("Token saved to {}", TOKEN_FILE);
        } catch (IOException e) {
            LOGGER.error("Failed to save token", e);
        }
    }

    /** Load token from file (XOR-decoded). */
    public static String loadToken() {
        if (cachedToken != null) return cachedToken;

        try {
            if (!Files.exists(TOKEN_FILE)) {
                LOGGER.debug("Token file not found: {}", TOKEN_FILE);
                return null;
            }
            try (Reader reader = Files.newBufferedReader(TOKEN_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has(KEY_TOKEN)) {
                    String obfuscated = json.get(KEY_TOKEN).getAsString();
                    cachedToken = decode(obfuscated);
                    LOGGER.debug("Token loaded from {}", TOKEN_FILE);
                    return cachedToken;
                }
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("Corrupted token file", e);
        } catch (IOException e) {
            LOGGER.error("Failed to load token", e);
        }
        return null;
    }

    /** Clear the saved token. */
    public static boolean clearToken() {
        cachedToken = null;
        try {
            boolean deleted = Files.deleteIfExists(TOKEN_FILE);
            LOGGER.info("Token cleared (file existed: {})", deleted);
            return deleted;
        } catch (IOException e) {
            LOGGER.error("Failed to clear token", e);
            return false;
        }
    }

    /**
     * Ограничить права доступа к файлу токена — только владелец.
     * На POSIX: chmod 600 (rw-------).
     * На Windows: убираем атрибуты чтения для всех, ставим hidden.
     */
    private static void restrictFilePermissions(Path path) {
        try {
            // POSIX-системы (Linux, macOS)
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(path, perms);
                LOGGER.debug("Set POSIX permissions to rw------- for {}", path);
            }
            // Windows — ставим атрибут «скрытый» и убираем «только чтение»
            else if (Files.getFileStore(path).supportsFileAttributeView("dos")) {
                DosFileAttributeView dosView = Files.getFileAttributeView(path, DosFileAttributeView.class);
                if (dosView != null) {
                    dosView.setHidden(true);
                    dosView.setReadOnly(false);
                    LOGGER.debug("Set Windows hidden attribute for {}", path);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not restrict file permissions for {}: {}", path, e.getMessage());
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("File attribute views not supported for {}, skipping permission restriction", path);
        }
    }

    /** XOR-encoding токена (Base64) — предотвращает чтение из файла на беглый взгляд. */
    private static String encode(String plaintext) {
        byte[] bytes = plaintext.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= ENCODING_KEY[i % ENCODING_KEY.length];
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Декодирование токена. */
    private static String decode(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= ENCODING_KEY[i % ENCODING_KEY.length];
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
