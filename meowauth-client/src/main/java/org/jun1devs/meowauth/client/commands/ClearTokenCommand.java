package org.jun1devs.meowauth.client.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jun1devs.meowauth.client.TokenReceiver;

/**
 * Клиентская команда /cleartoken — удаляет сохранённый токен.
 */
public class ClearTokenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cleartoken")
                .executes(ClearTokenCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;

        boolean cleared = TokenReceiver.clearToken();
        if (cleared) {
            mc.player.sendSystemMessage(Component.literal("§a[MeowAuth] Токен удалён."));
        } else {
            mc.player.sendSystemMessage(Component.literal("§e[MeowAuth] Токен не найден или не удалось удалить."));
        }

        return 1;
    }
}
