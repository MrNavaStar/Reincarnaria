package me.mrnavastar.reincarnaria;

import com.mojang.brigadier.CommandDispatcher;
import me.mrnavastar.reincarnaria.services.DeadPlayerService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;

public class Reincarnaria implements ModInitializer {

    public static PlayerManager playerManager;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            playerManager = server.getPlayerManager();
            CommandDispatcher<ServerCommandSource> commandDispatcher = server.getCommandManager().getDispatcher();

            DeadPlayerService.init(commandDispatcher);
        });
    }
}
