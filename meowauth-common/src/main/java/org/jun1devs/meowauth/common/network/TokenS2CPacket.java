package org.jun1devs.meowauth.common.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Пакет сервер -> клиент: содержит токен для сохранения на клиенте.
 */
public record TokenS2CPacket(String token) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token, 32767);
    }

    public static TokenS2CPacket decode(FriendlyByteBuf buf) {
        return new TokenS2CPacket(buf.readUtf(32767));
    }

    public String getToken() { return token; }
}
