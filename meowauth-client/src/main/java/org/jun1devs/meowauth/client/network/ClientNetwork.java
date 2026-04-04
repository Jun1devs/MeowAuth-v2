package org.jun1devs.meowauth.client.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jun1devs.meowauth.client.TokenReceiver;
import org.jun1devs.meowauth.common.network.NetworkConstants;
import org.jun1devs.meowauth.common.network.TokenC2SPacket;
import org.jun1devs.meowauth.common.network.TokenS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OnlyIn(Dist.CLIENT)
public class ClientNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientNetwork.class);
    private static boolean hasSentInitialPacket = false;


    private ClientNetwork() {
        // Utility class, no instances
    }

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(NetworkConstants.CHANNEL_NAME)
            .networkProtocolVersion(() -> NetworkConstants.PROTOCOL_VERSION)
            .clientAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .simpleChannel();

    @OnlyIn(Dist.CLIENT)
    public static void register() {
        LOGGER.info("Registering client network channels");

        // 1. Обработка получения токена от сервера (S2C)
        CHANNEL.messageBuilder(TokenS2CPacket.class, NetworkConstants.S2C_TOKEN_PACKET_ID)
                .encoder(TokenS2CPacket::encode)
                .decoder(TokenS2CPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> {
                    String token = pkt.getToken();
                    TokenReceiver.saveToken(token);
                    LOGGER.info("Received and saved new token from server");
                    ctxSupplier.get().setPacketHandled(true);
                })
                .add();

        // 2. Пустой обработчик для C2S (клиент не принимает свои же пакеты)
        CHANNEL.messageBuilder(TokenC2SPacket.class, NetworkConstants.C2S_TOKEN_PACKET_ID)
                .encoder(TokenC2SPacket::encode)
                .decoder(TokenC2SPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> ctxSupplier.get().setPacketHandled(true))
                .add();
    }

    // 3. МЕТОД-ОБРАБОТЧИК: Срабатывает, когда игрок ЗАХОДИТ на сервер
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (hasSentInitialPacket) return; // Защита от повторной отправки

        String playerName = event.getPlayer().getName().getString();
        String savedToken = TokenReceiver.loadToken();
        // Если токена нет, шлем пустую строку (сервер кикнет, если игрок уже был в базе)
        String tokenToSend = (savedToken != null && !savedToken.isBlank()) ? savedToken : "";

        CHANNEL.sendToServer(new TokenC2SPacket(playerName, tokenToSend));
        LOGGER.debug("Sent initial auth packet for '{}' (hasToken: {})", playerName, !tokenToSend.isEmpty());

        hasSentInitialPacket = true;
    }

    // 4. МЕТОД-ОБРАБОТЧИК: Срабатывает при ВЫХОДЕ (сбрасываем флаг)
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        hasSentInitialPacket = false;
    }
}
