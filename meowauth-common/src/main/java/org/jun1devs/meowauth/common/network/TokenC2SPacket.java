package org.jun1devs.meowauth.common.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Пакет клиент -> сервер: содержит имя пользователя и токен для проверки.
 */
public record TokenC2SPacket(String username, String token) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(username, 32767);
        buf.writeUtf(token, 32767);
    }

    public static TokenC2SPacket decode(FriendlyByteBuf buf) {
        return new TokenC2SPacket(buf.readUtf(32767), buf.readUtf(32767));
    }

    public String getUsername() { return username; }
    public String getToken() { return token; }
}
