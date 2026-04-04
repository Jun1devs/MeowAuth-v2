package org.jun1devs.meowauth.common.network;

import net.minecraft.resources.ResourceLocation;

/**
 * Константы сетевого протокола, общие для клиента и сервера.
 */
public final class NetworkConstants {

    private NetworkConstants() {}

    public static final String PROTOCOL_VERSION = "2";
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation("meowauth", "main");

    /** ID пакетов для регистрации */
    public static final int C2S_TOKEN_PACKET_ID = 0;
    public static final int S2C_TOKEN_PACKET_ID = 1;
}
