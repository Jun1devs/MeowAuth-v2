package org.jun1devs.meowauth.server;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.jun1devs.meowauth.server.network.ServerNetwork;
import org.jun1devs.meowauth.server.data.UserDataManager;
import org.jun1devs.meowauth.server.data.LockoutManager;
import org.jun1devs.meowauth.server.commands.AuthStatusCommand;
import org.jun1devs.meowauth.server.commands.MeowAuthCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

@Mod(MeowAuthServer.MOD_ID)
public class MeowAuthServer {
    public static final String MOD_ID = "meowauth_server";
    private static final Logger LOGGER = LoggerFactory.getLogger(MeowAuthServer.class);

    public MeowAuthServer() {
        String version = MeowAuthServer.class.getPackage().getImplementationVersion();
        if (version == null) version = "dev";
        LOGGER.info("MeowAuth Server v{} initializing...", version);

        // Subscribe to initialization events
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        // PlayerJoinHandler is registered via @Mod.EventBusSubscriber — no explicit registration needed
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        LOGGER.info("MeowAuth Server mod loaded");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ConfigManager.load();
            UserDataManager.setDataFile(Paths.get(ConfigManager.getDataFile()));
            LockoutManager.load();
            ServerNetwork.register();
            LOGGER.info("Server network initialized, data file: {}", ConfigManager.getDataFile());
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AuthStatusCommand.register(event.getDispatcher());
        MeowAuthCommand.register(event.getDispatcher());
        LOGGER.debug("Server commands registered");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Shutting down MeowAuth data managers...");
        LockoutManager.shutdown();
        UserDataManager.shutdown();
    }
}
