package me.mrnavastar.reincarnaria.services.distribution;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.mrnavastar.reincarnaria.Reincarnaria;
import me.mrnavastar.reincarnaria.services.party.Party;
import me.mrnavastar.reincarnaria.services.party.PartyService;
import me.mrnavastar.reincarnaria.util.ChatUtil;
import mrnavastar.sqlib.DataContainer;
import mrnavastar.sqlib.Table;
import mrnavastar.sqlib.database.Database;
import mrnavastar.sqlib.sql.SQLDataType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class DistributionService {

    private static Table spawnPoints;
    private static Table partyLookupTable;
    private static Table partyDataTable;

    private static void registerCommands(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(CommandManager.literal("spreader")

            .then(CommandManager.literal("addSpawn")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .then(CommandManager.argument("radius", IntegerArgumentType.integer())
                        .executes(ctx -> addSpawnPoint(ctx, StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "radius")))
                    )
                )
            )

            .then(CommandManager.literal("list").executes(DistributionService::list))
        );
    }

    private static int addSpawnPoint(CommandContext<ServerCommandSource> context, String name, int radius) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        exec.sendMessage(ChatUtil.newMessage("Added: " + name));
        DataContainer spawnPoint = spawnPoints.createDataContainer(name);
        spawnPoint.put("pos", exec.getBlockPos());
        spawnPoint.put("radius", radius);
        return 0;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        spawnPoints.getIds().forEach(id -> exec.sendMessage(ChatUtil.newMessage(id)));
        return 0;
    }

    private static int distribute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        ArrayList<UUID> singles = new ArrayList<>();
        Reincarnaria.userCache.load().forEach(entry -> singles.add(entry.getProfile().getId()));

        int maxTeams = spawnPoints.getIds().size();
        int maxTeamSize = Math.round((float) singles.size() / maxTeams);

        ArrayList<Party> parties = new ArrayList<>();
        for (DataContainer partyData: partyDataTable.getDataContainers()) {
            Party party = Reincarnaria.GSON.fromJson(partyData.getJson("partyData"), Party.class);
            singles.removeAll(party.getMembers());
            parties.add(party);
        }

        parties.sort(Comparator.comparingInt(Party::getSize));
        parties.forEach(party -> System.out.println(party.getSize()));

        //ArrayList<DataContainer> spawns = (ArrayList<DataContainer>) spawnPoints.getDataContainers().stream().toList();
        ArrayList<ArrayList<UUID>> teams = new ArrayList<>(maxTeams);
        for (int i = 0; i < maxTeams; i++) teams.add(new ArrayList<>());

        int index = 0;
        spawnPoints.beginTransaction();
        loop:
        for (Party party : parties) {
            if (maxTeamSize - teams.get(index).size() >= party.getSize()) {
                teams.get(index).addAll(party.getMembers());
                index = (index + 1) % maxTeams;
                continue;
            }

            while (maxTeamSize - teams.get(index).size() < party.getSize()) {
                index++;
                if (index > teams.size() - 1) {
                    index = -1;
                    break loop;
                }
            }
        }

        if (index == -1) {
            ChatUtil.ERROR(exec, "Failed, sorry bozo");
            return 1;
        }



        spawnPoints.endTransaction();
        return 0;
    }

    public static void init(MinecraftServer mcServer, Database database) {
        registerCommands(mcServer.getCommandFunctionManager().getDispatcher());

        spawnPoints = database.createTable("spawnpoints").addColumn("pos", SQLDataType.BLOCKPOS).addColumn("radius", SQLDataType.INT).addColumn("players", SQLDataType.JSON).finish();
        partyLookupTable = database.createTable("partyLookup").addColumn("partyId", SQLDataType.UUID).addColumn("invites", SQLDataType.JSON).finish();
        partyDataTable = database.createTable("partyData").addColumn("partyData", SQLDataType.JSON).finish();

        /*ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            Party party = PartyService.lookupParty(partyLookupTable, player.getUuid());
            if (party == null) return;



        });*/
    }
}
