package dev.waco0311.cosmowarp.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Purely client-side command -- no server round trip, no permission needed -- since the bug this
 * exists for (a stuck hyperspace screen effect) is a client-only visual state issue. Works even
 * for the local player in singleplayer immediately, with no dependency on server state.
 */
public class ModClientCommands {

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("cosmowarp")
                .then(Commands.literal("clearhyperspace")
                        .executes(ctx -> {
                            WarpEffectClient.clear();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("command.cosmowarp.clearhyperspace.success"),
                                    false);
                            return 1;
                        })));
    }
}
