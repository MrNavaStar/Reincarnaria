package me.mrnavastar.reincarnaria;

import com.google.gson.Gson;
import me.mrnavastar.reincarnaria.services.DeadPlayerService;
import me.mrnavastar.reincarnaria.services.PartyService;
import mrnavastar.sqlib.database.Database;
import mrnavastar.sqlib.database.SQLiteDatabase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.UserCache;

public class Reincarnaria implements ModInitializer {

    public static String MOD_ID = "reincarnaria";
    public static Gson GSON = new Gson();
    public static PlayerManager playerManager;
    public static UserCache userCache;

    @Override
    public void onInitialize() {
        Database database = new SQLiteDatabase(Reincarnaria.MOD_ID, "Reincarnaria", "config");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            playerManager = server.getPlayerManager();
            userCache = server.getUserCache();

            DeadPlayerService.init(server);
            PartyService.init(server, database);
        });
    }
}