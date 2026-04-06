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
        String version = MeowAuthClient.class.getPackage().getImplementationVersion();
        if (version == null) version = "dev";
        LOGGER.info("MeowAuth Client v{} initializing...", version);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        // Token send on login/logout
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> ClientNetwork.onPlayerLogin(e));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientNetwork.onPlayerLogout(e));

        // Handle client-side commands (e.g. /cleartoken)
        MinecraftForge.EVENT_BUS.addListener(this::onClientChat);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                LOGGER.info("MeowAuth Client mod loaded (dist: CLIENT)")
        );
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Register network channels
        event.enqueueWork(() ->
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientNetwork::register)
        );
    }

    /** Intercept client-side slash-commands (e.g. /cleartoken). */
    private void onClientChat(ClientChatEvent event) {
        if (event.getMessage().trim().equalsIgnoreCase("/cleartoken")) {
            boolean cleared = TokenReceiver.clearToken();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal(cleared ? "§a[MeowAuth] §fToken removed." : "§e[MeowAuth] §fToken not found or already removed."
                        ),
                        false
                );
            }
            event.setCanceled(true); // Prevent sending to server
        }
    }
}
