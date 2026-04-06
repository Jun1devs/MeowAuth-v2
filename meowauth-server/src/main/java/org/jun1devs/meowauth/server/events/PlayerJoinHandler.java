package org.jun1devs.meowauth.server.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jun1devs.meowauth.server.MeowColors;
import org.jun1devs.meowauth.server.network.ServerNetwork;
import org.jun1devs.meowauth.server.data.UserDataManager;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.MeowAuthServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MeowAuthServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerJoinHandler.class);

    /** Игроки, уже аутентифицированные в текущей сессии (сбрасывается при выходе). */
    private static final Set<String> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    /** Игроки, которым выдан токен, но они ещё не прошли аутентификацию (для freeze). */
    private static final Set<String> pendingAuthPlayers = ConcurrentHashMap.newKeySet();

    /** Эффект невидимости + immobility для неаутентифицированных игроков. */
    private static final MobEffectInstance AUTH_FREEZE = new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 255, false, false);

    @SubscribeEvent
    public static void onJoin(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String username = player.getName().getString();

        // If player already authenticated via C2S — remove restrictions
        if (authenticatedPlayers.contains(username)) {
            pendingAuthPlayers.remove(username);
            removeAuthRestrictions(player);
            LOGGER.debug("Player '{}' already authenticated via C2S, restrictions removed", username);
            return;
        }

        // Attempt atomic registration of new player
        String token = UserDataManager.getOrRegisterUser(username, ConfigManager.getTokenLength());
        if (token != null) {
            // New player — issue token and freeze
            LOGGER.info("New player '{}' joined, registering and issuing auth token", username);
            ServerNetwork.sendTokenToClient(player, token);
            player.sendSystemMessage(Component.literal(MeowColors.GOLD + MeowColors.PREFIX + MeowColors.WHITE + "Authentication token issued."));
            pendingAuthPlayers.add(username);
            applyAuthRestrictions(player);

            if (ConfigManager.isDebug()) {
                LOGGER.debug("Token issued for new player '{}' (length: {})", username, token.length());
            }
            return;
        }

        // Player already registered — await C2S packet, also freeze
        LOGGER.debug("Player '{}' is registered, awaiting C2S token packet", username);
        pendingAuthPlayers.add(username);
        applyAuthRestrictions(player);
    }

    /** Очистка состояния при выходе игрока. */
    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String username = player.getName().getString();
        authenticatedPlayers.remove(username);
        pendingAuthPlayers.remove(username);
        LOGGER.debug("Player '{}' logged out, auth state cleared", username);
    }

    /** Блокировка взаимодействия для неаутентифицированных игроков. */
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String username = player.getName().getString();
        if (pendingAuthPlayers.contains(username)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(MeowColors.RED + MeowColors.PREFIX + MeowColors.WHITE + "Complete authentication first."));
        }
    }

    /** Применить ограничения для неаутентифицированного игрока. */
    private static void applyAuthRestrictions(ServerPlayer player) {
        player.addEffect(AUTH_FREEZE);
        player.setInvulnerable(true);
    }

    /** Снять ограничения после аутентификации. */
    private static void removeAuthRestrictions(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.setInvulnerable(false);
    }

    /**
     * Mark player as authenticated (called from ServerNetwork).
     * Schedules restriction removal on the main tick thread.
     */
    public static void markAuthenticated(String username) {
        authenticatedPlayers.add(username);
        pendingAuthPlayers.remove(username);
        // Schedule on main tick thread — entity modification must not run on network thread
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                var player = server.getPlayerList().getPlayerByName(username);
                if (player != null) {
                    removeAuthRestrictions(player);
                }
            });
        }
    }
}
