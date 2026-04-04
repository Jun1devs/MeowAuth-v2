package org.jun1devs.meowauth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jun1devs.meowauth.client.network.ClientNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MeowAuthClient.MOD_ID)
public class MeowAuthClient {
    public static final String MOD_ID = "meowauth_client";
    private static final Logger LOGGER = LoggerFactory.getLogger(MeowAuthClient.class);

    public MeowAuthClient() {
        LOGGER.info("MeowAuth Client v2.0.1 initializing...");

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        // 1. Отправка токена при входе/выходе игрока
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> ClientNetwork.onPlayerLogin(e));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientNetwork.onPlayerLogout(e));

        // 2. Обработка клиентских команд (например, /cleartoken)
        MinecraftForge.EVENT_BUS.addListener(this::onClientChat);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                LOGGER.info("MeowAuth Client mod loaded (dist: CLIENT)")
        );
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Регистрируем сетевые каналы
        event.enqueueWork(() ->
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientNetwork::register)
        );
    }

    /** Перехватывает команды, начинающиеся с /, чтобы обработать их на клиенте */
    private void onClientChat(ClientChatEvent event) {
        if (event.getMessage().trim().equalsIgnoreCase("/cleartoken")) {
            boolean cleared = TokenReceiver.clearToken();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal(cleared ? "§a[MeowAuth] Токен удалён." : "§e[MeowAuth] Токен не найден или уже удалён."
                        ),
                        false
                );
            }
            event.setCanceled(true); // Запрещаем отправку текста в чат серверу
        }
    }
}
