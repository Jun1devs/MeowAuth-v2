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

        CHANNEL.messageBuilder(TokenS2CPacket.class, NetworkConstants.S2C_TOKEN_PACKET_ID)
                .encoder(TokenS2CPacket::encode)
                .decoder(TokenS2CPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> ctxSupplier.get().setPacketHandled(true))
                .add();
    }

    private static void handleTokenC2S(TokenC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            String packetUsername = pkt.getUsername();
            String token = pkt.getToken();

            // 1. Валидация: имя из пакета должно совпадать с реальным именем игрока
            String actualUsername = sender.getName().getString();
            if (!actualUsername.equals(packetUsername)) {
                LOGGER.warn("Player '{}' sent C2S packet with username '{}', disconnecting",
                        actualUsername, packetUsername);
                sender.connection.disconnect(Component.literal("§c[MeowAuth] Username mismatch."));
                return;
            }

            // 2. Если токен верный — аутентифицируем и выходим
            if (!token.isEmpty() && UserDataManager.verifyToken(actualUsername, token)) {
                PlayerJoinHandler.resetAttempts(actualUsername);
                PlayerJoinHandler.markAuthenticated(actualUsername);
                LOGGER.info("Player '{}' authenticated successfully.", actualUsername);
                return;
            }

            // 3. Токен неверный или отсутствует
            if (PlayerJoinHandler.isLockedOut(actualUsername)) {
                sender.connection.disconnect(Component.literal("§cToo many failed attempts. Contact an admin."));
                return;
            }

            boolean isRegistered = UserDataManager.isRegistered(actualUsername);

            if (!isRegistered) {
                // Игрок НОВЫЙ — но PlayerJoinHandler уже должен был его зарегистрировать.
                // На случай если C2S пришёл раньше: регистрируем сейчас.
                PlayerJoinHandler.recordFailedAttempt(actualUsername);
                String newToken = UserDataManager.registerNewUser(actualUsername, ConfigManager.getTokenLength());
                ServerNetwork.sendTokenToClient(sender, newToken);
                PlayerJoinHandler.markAuthenticated(actualUsername);
                LOGGER.info("New player '{}' registered and issued token via C2S.", actualUsername);
                return;
            }

            // 4. Игрок старый, но токен не подошёл — записываем неудачную попытку
            PlayerJoinHandler.recordFailedAttempt(actualUsername);
            LOGGER.warn("Token mismatch for '{}'. Awaiting server-side re-issue on next join.", actualUsername);
        });
        ctx.setPacketHandled(true);
    }

    public static void sendTokenToClient(ServerPlayer player, String token) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TokenS2CPacket(token));
    }
}
