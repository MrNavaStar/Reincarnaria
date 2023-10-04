package me.mrnavastar.reincarnaria;

import me.mrnavastar.reincarnaria.services.DeadPlayerService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.PlayerManager;

public class Reincarnaria implements ModInitializer {

    public static PlayerManager playerManager;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> playerManager = server.getPlayerManager());

        DeadPlayerService.init();
    }
}
