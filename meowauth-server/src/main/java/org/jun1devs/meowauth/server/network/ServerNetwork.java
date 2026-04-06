package org.jun1devs.meowauth.server.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jun1devs.meowauth.common.network.NetworkConstants;
import org.jun1devs.meowauth.common.network.TokenC2SPacket;
import org.jun1devs.meowauth.common.network.TokenS2CPacket;
import org.jun1devs.meowauth.server.MeowColors;
import org.jun1devs.meowauth.server.data.UserDataManager;
import org.jun1devs.meowauth.server.data.LockoutManager;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.events.PlayerJoinHandler;
import org.jun1devs.meowauth.server.security.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class ServerNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerNetwork.class);

    /** Minecraft username max length. */
    private static final int MAX_USERNAME_LENGTH = 16;

    /** Dummy token for timing-attack prevention (takes ~300ms at BCrypt cost 12). */
    private static final String DUMMY_TOKEN = "dummy-timing-prevention-token-12345";

    private ServerNetwork() {
        // Utility class, no instances
    }

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(NetworkConstants.CHANNEL_NAME)
            .networkProtocolVersion(() -> NetworkConstants.PROTOCOL_VERSION)
            .clientAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .simpleChannel();

    public static void register() {
        LOGGER.info("Registering server network channels (STRICT MODE)");

        CHANNEL.messageBuilder(TokenC2SPacket.class, NetworkConstants.C2S_TOKEN_PACKET_ID)
                .encoder(TokenC2SPacket::encode)
                .decoder(TokenC2SPacket::decode)
                .consumerMainThread(ServerNetwork::handleTokenC2S)
                .add();
    }

    private static void handleTokenC2S(TokenC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            String packetUsername = pkt.getUsername();
            String token = pkt.getToken();

            // 1. Validate username length (Minecraft limit = 16 chars)
            if (packetUsername == null || packetUsername.length() > MAX_USERNAME_LENGTH) {
                LOGGER.warn("Player sent invalid username length: '{}', disconnecting", packetUsername);
                sender.connection.disconnect(Component.literal(MeowColors.RED + MeowColors.PREFIX + "Invalid username."));
                return;
            }

            // 2. Validate: packet username must match actual player username
            String actualUsername = sender.getName().getString();
            if (!actualUsername.equals(packetUsername)) {
                LOGGER.warn("Player '{}' sent C2S packet with username '{}', disconnecting",
                        actualUsername, packetUsername);
                sender.connection.disconnect(Component.literal(MeowColors.RED + MeowColors.PREFIX + "Username mismatch."));
                return;
            }

            // 2. Timing-safe authentication: ALWAYS perform exactly one BCrypt operation
            //    to prevent username enumeration via timing.
            boolean authenticated;
            if (!token.isEmpty() && UserDataManager.isRegistered(actualUsername)) {
                // Path A: user exists, token exists → verify (BCrypt checkpw)
                authenticated = UserDataManager.verifyToken(actualUsername, token);
            } else if (!token.isEmpty()) {
                // Path B: user NOT found, but token is non-empty → dummy hash
                //    (takes ~300ms, same as real verify)
                HashUtil.hash(DUMMY_TOKEN);
                authenticated = false;
            } else {
                // Path C: empty token → dummy hash for unknown users
                HashUtil.hash(DUMMY_TOKEN);
                authenticated = false;
            }

            if (authenticated) {
                LockoutManager.resetAttempts(actualUsername);
                PlayerJoinHandler.markAuthenticated(actualUsername);
                LOGGER.info("Player '{}' authenticated successfully.", actualUsername);
                return;
            }

            // 3. Token incorrect or missing
            if (LockoutManager.isLockedOut(actualUsername)) {
                sender.connection.disconnect(Component.literal(MeowColors.RED + "Too many failed attempts. Contact an admin."));
                return;
            }

            // 4. Player not registered — issue token
            boolean isRegistered = UserDataManager.isRegistered(actualUsername);
            if (!isRegistered) {
                String newToken = UserDataManager.getOrRegisterUser(actualUsername, ConfigManager.getTokenLength());
                if (newToken != null) {
                    ServerNetwork.sendTokenToClient(sender, newToken);
                    PlayerJoinHandler.markAuthenticated(actualUsername);
                    LOGGER.info("New player '{}' registered via C2S fallback and issued token.", actualUsername);
                }
                return;
            }

            // 5. Existing player, token mismatch — record failed attempt
            LockoutManager.recordFailedAttempt(actualUsername);
            LOGGER.warn("Token mismatch for '{}'. Awaiting server-side re-issue on next join.", actualUsername);
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Отправить токен клиенту.
     * SECURITY NOTE: Токен передаётся в открытом виде. Minecraft использует
     * шифрование для игровых сессий, но токен остаётся уязвимым при перехвате.
     * Будущее улучшение: challenge-response механизм или ротация токенов.
     */
    public static void sendTokenToClient(ServerPlayer player, String token) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TokenS2CPacket(token));
    }
}
