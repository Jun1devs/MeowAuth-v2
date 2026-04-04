package org.jun1devs.meowauth.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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

        // 1. СРАЗУ отправляем данные на сервер при инициализации
        String playerName = Minecraft.getInstance().getUser().getName();
        String savedToken = TokenReceiver.loadToken();
        // Если токена нет, шлем пустую строку, но с РЕАЛЬНЫМ ником
        String tokenToSend = (savedToken != null && !savedToken.isBlank()) ? savedToken : "";

        CHANNEL.sendToServer(new TokenC2SPacket(playerName, tokenToSend));
        LOGGER.debug("Sent initial auth packet for '{}' (hasToken: {})", playerName, !tokenToSend.isEmpty());

        // 2. Обработка ответа от сервера (например, новый токен после /meowauth reset)
        CHANNEL.messageBuilder(TokenS2CPacket.class, NetworkConstants.S2C_TOKEN_PACKET_ID)
                .encoder(TokenS2CPacket::encode)
                .decoder(TokenS2CPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> {
                    String token = pkt.getToken();
                    TokenReceiver.saveToken(token);
                    LOGGER.info("Received and saved new token from server");

                    // Сразу отправляем полученный токен обратно для входа
                    String user = Minecraft.getInstance().getUser().getName();
                    CHANNEL.sendToServer(new TokenC2SPacket(user, token));

                    ctxSupplier.get().setPacketHandled(true);
                })
                .add();

        // C2S: клиент -> сервер (не обрабатывается на клиенте)
        CHANNEL.messageBuilder(TokenC2SPacket.class, NetworkConstants.C2S_TOKEN_PACKET_ID)
                .encoder(TokenC2SPacket::encode)
                .decoder(TokenC2SPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> ctxSupplier.get().setPacketHandled(true))
                .add();
    }
}
