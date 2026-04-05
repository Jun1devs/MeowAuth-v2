package org.jun1devs.meowauth.server.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jun1devs.meowauth.server.network.ServerNetwork;
import org.jun1devs.meowauth.common.UserDataManager;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.MeowAuthServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MeowAuthServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerJoinHandler.class);

    /** Отслеживание попыток входа: username -> (count, lastAttempt) */
    private static final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    /** Игроки, уже аутентифицированные в текущей сессии (сбрасывается при выходе). */
    private static final Set<String> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onJoin(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String username = player.getName().getString();

        // Если игрок уже аутентифицирован через C2S-пакет — ничего не делаем
        if (authenticatedPlayers.contains(username)) {
            LOGGER.debug("Player '{}' already authenticated via C2S, skipping", username);
            return;
        }

        // Если игрок уже зарегистрирован — ждём C2S-пакет от клиента
        if (UserDataManager.isRegistered(username)) {
            LOGGER.debug("Player '{}' is registered, awaiting C2S token packet", username);
            return;
        }

        // Новый игрок — регистрируем и выдаём токен
        LOGGER.info("New player '{}' joined, registering and issuing auth token", username);
        String token = UserDataManager.registerNewUser(username, ConfigManager.getTokenLength());
        ServerNetwork.sendTokenToClient(player, token);
        player.sendSystemMessage(Component.literal("§6[MeowAuth] §fAuthentication token issued."));
        authenticatedPlayers.add(username);

        if (ConfigManager.isDebug()) {
            LOGGER.debug("Token issued for new player '{}' (length: {})", username, token.length());
        }
    }

    /**
     * Отметить игрока как аутентифицированного (вызывается из ServerNetwork).
     */
    public static void markAuthenticated(String username) {
        authenticatedPlayers.add(username);
    }

    /**
     * Проверить, заблокирован ли пользователь после множества неудачных попыток.
     */
    public static boolean isLockedOut(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) return false;

        if (attempt.count >= ConfigManager.getMaxLoginAttempts()) {
            long elapsed = Instant.now().getEpochSecond() - attempt.lastAttemptEpoch;
            if (elapsed < ConfigManager.getLockoutDurationSeconds()) {
                LOGGER.warn("User '{}' is locked out ({} attempts, {}s remaining)",
                        username, attempt.count, ConfigManager.getLockoutDurationSeconds() - elapsed);
                return true;
            } else {
                // Срок блокировки истёк — сброс
                loginAttempts.remove(username);
                LOGGER.info("User '{}' lockout expired", username);
            }
        }
        return false;
    }

    /** Записать неудачную попытку входа. */
    public static void recordFailedAttempt(String username) {
        loginAttempts.compute(username, (key, existing) -> {
            if (existing == null) {
                return new LoginAttempt(1, Instant.now().getEpochSecond());
            }
            return new LoginAttempt(existing.count + 1, Instant.now().getEpochSecond());
        });
    }

    /** Сбросить счётчик после успешного входа. */
    public static void resetAttempts(String username) {
        loginAttempts.remove(username);
    }

    private record LoginAttempt(int count, long lastAttemptEpoch) {}
}
