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
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MeowAuthServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerJoinHandler.class);

    /** Отслеживание попыток входа: username -> (count, lastAttempt) */
    private static final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onJoin(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String username = player.getName().getString();
        LOGGER.info("Player '{}' joined, issuing auth token", username);

        // Регистрация/получение токена
        String token = UserDataManager.registerOrGetHash(username, ConfigManager.getTokenLength());

        // Отправка токена клиенту
        ServerNetwork.sendTokenToClient(player, token);

        // Сообщение игроку (без показа токена)
        player.sendSystemMessage(Component.literal("§6[MeowAuth] §fAuthentication token issued."));

        if (ConfigManager.isDebug()) {
            LOGGER.debug("Token issued for '{}' (length: {})", username, token.length());
        }
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
