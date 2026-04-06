package org.jun1devs.meowauth.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import org.jun1devs.meowauth.server.MeowColors;
import org.jun1devs.meowauth.server.data.UserDataManager;
import org.jun1devs.meowauth.server.data.LockoutManager;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.network.ServerNetwork;
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
        int refreshed = 0;
        var server = ctx.getSource().getServer();

        for (com.mojang.authlib.GameProfile profile : targets) {
            // profile.getName() may be null for offline-mode players
            String username = profile.getName();
            if (username == null) {
                // Try to resolve name from server cache by UUID
                var cachedProfile = server.getProfileCache().get(profile.getId());
                if (cachedProfile.isPresent()) {
                    username = cachedProfile.get().getName();
                }
            }
            if (username == null || username.isBlank()) continue;

            if (!UserDataManager.isRegistered(username)) {
                ctx.getSource().sendFailure(Component.literal(MeowColors.RED + MeowColors.PREFIX + "Player §e" + username + MeowColors.RED + " is not registered."));
                continue;
            }

            String newToken = UserDataManager.refreshToken(username, ConfigManager.getTokenLength());
            LockoutManager.resetAttempts(username);

            if (newToken != null) {
                // If player is online — send new token immediately
                var player = server.getPlayerList().getPlayer(profile.getId());
                if (player != null) {
                    ServerNetwork.sendTokenToClient(player, newToken);
                }
                LOGGER.info("Admin reset player '{}'. New token issued.", username);
                final String finalUsername = username;
                ctx.getSource().sendSuccess(() -> Component.literal(MeowColors.GREEN + MeowColors.PREFIX + "Token for §e" + finalUsername + MeowColors.GREEN + " updated."), true);
                refreshed++;
            }
        }

        if (refreshed == 0) {
            ctx.getSource().sendFailure(Component.literal(MeowColors.RED + MeowColors.PREFIX + "Failed to update tokens."));
        }
        return refreshed;
    }
}
