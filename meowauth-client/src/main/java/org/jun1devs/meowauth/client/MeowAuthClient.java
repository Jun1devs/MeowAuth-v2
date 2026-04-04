package org.jun1devs.meowauth.client;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.jun1devs.meowauth.client.network.ClientNetwork;
import org.jun1devs.meowauth.client.commands.ClearTokenCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MeowAuthClient.MOD_ID)
public class MeowAuthClient {
    public static final String MOD_ID = "meowauth_client";
    private static final Logger LOGGER = LoggerFactory.getLogger(MeowAuthClient.class);

    public MeowAuthClient() {
        LOGGER.info("MeowAuth Client v{} initializing...", "2.0.0");

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        // Регистрация клиентских команд
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                LOGGER.info("MeowAuth Client mod loaded (dist: CLIENT)")
        );
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientNetwork::register)
        );
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                ClearTokenCommand.register(event.getDispatcher())
        );
    }
}
