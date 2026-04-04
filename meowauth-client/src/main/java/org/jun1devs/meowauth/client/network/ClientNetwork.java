package org.jun1devs.meowauth.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
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

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(NetworkConstants.CHANNEL_NAME)
            .networkProtocolVersion(() -> NetworkConstants.PROTOCOL_VERSION)
            .clientAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(NetworkConstants.PROTOCOL_VERSION::equals)
            .simpleChannel();

    @OnlyIn(Dist.CLIENT)
    public static void register() {
        LOGGER.info("Registering client network channels");

        // S2C: сервер -> клиент (получение токена)
        CHANNEL.messageBuilder(TokenS2CPacket.class, NetworkConstants.S2C_TOKEN_PACKET_ID)
                .encoder(TokenS2CPacket::encode)
                .decoder(TokenS2CPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    String token = pkt.getToken();
                    TokenReceiver.saveToken(token);
                    LOGGER.debug("Token received and saved from server");

                    // Отправляем подтверждение обратно на сервер
                    String username = Minecraft.getInstance().getUser().getName();
                    CHANNEL.sendToServer(new TokenC2SPacket(username, token));
                    LOGGER.debug("Token confirmation sent to server");

                    ctx.setPacketHandled(true);
                })
                .add();

        // C2S: клиент -> сервер (не обрабатывается на клиенте)
        CHANNEL.messageBuilder(TokenC2SPacket.class, NetworkConstants.C2S_TOKEN_PACKET_ID)
                .encoder(TokenC2SPacket::encode)
                .decoder(TokenC2SPacket::decode)
                .consumerMainThread((pkt, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.setPacketHandled(true);
                })
                .add();
    }
}
