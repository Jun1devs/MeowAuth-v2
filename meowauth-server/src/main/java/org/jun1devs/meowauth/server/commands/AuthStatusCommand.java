package org.jun1devs.meowauth.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jun1devs.meowauth.common.UserDataManager;
import org.jun1devs.meowauth.server.events.PlayerJoinHandler;

/**
 * Команда /authstatus — показывает статус аутентификации игрока.
 */
public class AuthStatusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("authstatus")
                .executes(AuthStatusCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String username = player.getName().getString();

        boolean registered = UserDataManager.isRegistered(username);
        boolean lockedOut = PlayerJoinHandler.isLockedOut(username);

        if (lockedOut) {
            player.sendSystemMessage(Component.literal("§c[MeowAuth] Вы заблокированы. Подождите перед следующей попыткой."));
        } else if (registered) {
            player.sendSystemMessage(Component.literal("§a[MeowAuth] Вы зарегистрированы и аутентифицированы."));
        } else {
            player.sendSystemMessage(Component.literal("§e[MeowAuth] Вы не зарегистрированы. Токен будет выдан при следующем подключении."));
        }

        return 1;
    }
}
