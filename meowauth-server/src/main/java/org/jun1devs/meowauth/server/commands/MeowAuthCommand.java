package org.jun1devs.meowauth.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import org.jun1devs.meowauth.common.UserDataManager;
import org.jun1devs.meowauth.server.events.PlayerJoinHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class MeowAuthCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeowAuthCommand.class);

    private MeowAuthCommand() {
        // Utility class, no instances
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("meowauth")
                .requires(src -> src.hasPermission(4))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(MeowAuthCommand::resetPlayer)
                        )
                )
        );
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<com.mojang.authlib.GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "targets");

        for (com.mojang.authlib.GameProfile profile : targets) {
            String username = profile.getName();
            if (username == null || username.isBlank()) continue;

            UserDataManager.removeUser(username);
            PlayerJoinHandler.resetAttempts(username);

            LOGGER.info("Admin reset player '{}'. They will get a new token on next join.", username);
            ctx.getSource().sendSuccess(() -> Component.literal("§a[MeowAuth] Игрок §e" + username + " §a сброшен. При входе получит новый токен."), true);
        }
        return 1;
    }
}
