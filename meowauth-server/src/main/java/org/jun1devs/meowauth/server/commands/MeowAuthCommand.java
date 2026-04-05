package org.jun1devs.meowauth.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import org.jun1devs.meowauth.common.UserDataManager;
import org.jun1devs.meowauth.server.ConfigManager;
import org.jun1devs.meowauth.server.events.PlayerJoinHandler;
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

        for (com.mojang.authlib.GameProfile profile : targets) {
            String username = profile.getName();
            if (username == null || username.isBlank()) continue;

            if (!UserDataManager.isRegistered(username)) {
                ctx.getSource().sendFailure(Component.literal("§c[MeowAuth] Игрок §e" + username + "§c не зарегистрирован."));
                continue;
            }

            String newToken = UserDataManager.refreshToken(username, ConfigManager.getTokenLength());
            PlayerJoinHandler.resetAttempts(username);

            if (newToken != null) {
                // Если игрок онлайн — сразу отправляем новый токен
                var server = ctx.getSource().getServer();
                var player = server.getPlayerList().getPlayer(profile.getId());
                if (player != null) {
                    ServerNetwork.sendTokenToClient(player, newToken);
                }
                LOGGER.info("Admin reset player '{}'. New token issued.", username);
                ctx.getSource().sendSuccess(() -> Component.literal("§a[MeowAuth] Токен для §e" + username + "§a обновлён."), true);
                refreshed++;
            }
        }

        if (refreshed == 0) {
            ctx.getSource().sendFailure(Component.literal("§c[MeowAuth] Не удалось обновить токены."));
        }
        return refreshed;
    }
}
