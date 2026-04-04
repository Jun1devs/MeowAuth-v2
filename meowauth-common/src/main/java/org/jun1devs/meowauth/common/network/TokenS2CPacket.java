package org.jun1devs.meowauth.common.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Пакет сервер -> клиент: содержит токен для сохранения на клиенте.
 */
public class TokenS2CPacket {
    private final String token;

    public TokenS2CPacket(String token) {
        this.token = token;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token, 32767);
    }

    public static TokenS2CPacket decode(FriendlyByteBuf buf) {
        return new TokenS2CPacket(buf.readUtf(32767));
    }

    public String getToken() { return token; }
}
