package org.jun1devs.meowauth.server.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Утилиты для безопасного хеширования паролей и токенов.
 * Использует BCrypt — отраслевой стандарт хеширования.
 */
public final class HashUtil {

    private HashUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** BCrypt "cost factor" — чем выше, тем медленнее, но безопаснее. 12 — оптимален для 2024. */
    private static final int LOG_ROUNDS = 12;

    /** Минимальная длина токена/пароля */
    private static final int MIN_LENGTH = 6;

    /** Максимальная длина — защита от DoS */
    private static final int MAX_LENGTH = 72; // BCrypt limit

    /**
     * Хеширует токен/пароль через BCrypt.
     */
    public static String hash(String token) {
        validate(token);
        return BCrypt.hashpw(token, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Проверяет совпадение токена с хешем.
     */
    public static boolean verify(String token, String hash) {
        if (token == null || hash == null) return false;
        if (hash.length() != 60) return false; // BCrypt hash length
        return BCrypt.checkpw(token, hash);
    }

    /**
     * Валидация токена/пароля (внутренний метод).
     */
    private static void validate(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token must not be empty");
        }
        if (token.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Token too short (min " + MIN_LENGTH + ")");
        }
        if (token.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Token too long (max " + MAX_LENGTH + ")");
        }
    }
}
