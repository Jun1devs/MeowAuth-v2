package org.jun1devs.meowauth.server;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.jun1devs.meowauth.server.network.ServerNetwork;
import org.jun1devs.meowauth.common.UserDataManager;
import org.jun1devs.meowauth.server.events.PlayerJoinHandler;
import org.jun1devs.meowauth.server.commands.AuthStatusCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

@Mod(MeowAuthServer.MOD_ID)
public class MeowAuthServer {
    public static final String MOD_ID = "meowauth_server";
    private static final Logger LOGGER = LoggerFactory.getLogger(MeowAuthServer.class);

    public MeowAuthServer() {
        LOGGER.info("MeowAuth Server v{} initializing...", "2.0.0");

        // Подписка на события инициализации
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        // Регистрация обработчика событий игроков
        MinecraftForge.EVENT_BUS.register(PlayerJoinHandler.class);

        // Регистрация команд
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("MeowAuth Server mod loaded");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ConfigManager.load();
            UserDataManager.setDataFile(Paths.get(ConfigManager.dataFile));
            ServerNetwork.register();
            LOGGER.info("Server network initialized, data file: {}", ConfigManager.dataFile);
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AuthStatusCommand.register(event.getDispatcher());
        LOGGER.debug("Server commands registered");
    }
}
