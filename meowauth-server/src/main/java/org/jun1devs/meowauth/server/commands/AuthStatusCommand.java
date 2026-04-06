package org.jun1devs.meowauth.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jun1devs.meowauth.server.MeowColors;
import org.jun1devs.meowauth.server.data.UserDataManager;
import org.jun1devs.meowauth.server.data.LockoutManager;

/**
 * Команда /authstatus — показывает статус аутентификации игрока.
 */
public class AuthStatusCommand {

    private AuthStatusCommand() {
        // Utility class, no instances
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("authstatus")
                .requires(src -> src.hasPermission(0)) // Any player can check their own status
                .executes(AuthStatusCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String username = player.getName().getString();

        boolean registered = UserDataManager.isRegistered(username);
        boolean lockedOut = LockoutManager.isLockedOut(username);

        if (lockedOut) {
            player.sendSystemMessage(Component.literal(MeowColors.RED + MeowColors.PREFIX + "You are locked out. Wait before trying again."));
        } else if (registered) {
            player.sendSystemMessage(Component.literal(MeowColors.GREEN + MeowColors.PREFIX + "You are registered and authenticated."));
        } else {
            player.sendSystemMessage(Component.literal(MeowColors.YELLOW + MeowColors.PREFIX + "You are not registered. A token will be issued on next join."));
        }

        return 1;
    }
}
