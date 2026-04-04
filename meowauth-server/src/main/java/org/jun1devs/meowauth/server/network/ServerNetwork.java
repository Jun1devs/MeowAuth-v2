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
import org.jun1devs.meowauth.common.UserDataManager;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.events.PlayerJoinHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class ServerNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerNetwork.class);

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(NetworkConstants.CHANNEL_NAME)
            .networkProtocolVersion(() -> NetworkConstants.PROTOCOL_VERSION)
            .clientAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .simpleChannel();

    public static void register() {
        LOGGER.info("Registering server network channels");

        // C2S: клиент -> сервер (проверка токена)
        CHANNEL.messageBuilder(TokenC2SPacket.class, NetworkConstants.C2S_TOKEN_PACKET_ID)
                .encoder(TokenC2SPacket::encode)
                .decoder(TokenC2SPacket::decode)
                .consumerMainThread(ServerNetwork::handleTokenC2S)
                .add();

        // S2C: сервер -> клиент (доставка токена)
        CHANNEL.messageBuilder(TokenS2CPacket.class, NetworkConstants.S2C_TOKEN_PACKET_ID)
                .encoder(TokenS2CPacket::encode)
                .decoder(TokenS2CPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> {
                    ctxSupplier.get().setPacketHandled(true);
                })
                .add();
    }

    private static void handleTokenC2S(TokenC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            String username = pkt.getUsername();
            String token = pkt.getToken();

            if (PlayerJoinHandler.isLockedOut(username)) {
                sender.connection.disconnect(
                        Component.literal("§cToo many failed attempts. Try again later."));
                return;
            }

            boolean valid = UserDataManager.verifyToken(username, token);
            if (!valid) {
                PlayerJoinHandler.recordFailedAttempt(username);
                sender.connection.disconnect(Component.literal(ConfigManager.kickMessage));
                LOGGER.warn("Player '{}' failed token auth", username);
            } else {
                PlayerJoinHandler.resetAttempts(username);
                LOGGER.info("Player '{}' authenticated successfully", username);
            }
        });
        ctx.setPacketHandled(true);
    }

    /** Отправить токен конкретному игроку. */
    public static void sendTokenToClient(ServerPlayer player, String token) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TokenS2CPacket(token));
    }
}
