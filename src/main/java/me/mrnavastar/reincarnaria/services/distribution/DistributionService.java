package me.mrnavastar.reincarnaria.services.distribution;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.mrnavastar.reincarnaria.Reincarnaria;
import me.mrnavastar.reincarnaria.services.party.Party;
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
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class DistributionService {

    private static Table spawnPoints;
    private static Table playerData;
    private static Table partyDataTable;

    private static void registerCommands(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(CommandManager.literal("distribution")

            .then(CommandManager.literal("addSpawn")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .executes(ctx -> addSpawnPoint(ctx, StringArgumentType.getString(ctx, "name")))
                )
            )

            .then(CommandManager.literal("removeSpawn")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .executes(ctx -> removeSpawnPoint(ctx, StringArgumentType.getString(ctx, "name")))
                )
            )

            .then(CommandManager.literal("list").executes(DistributionService::list))

            .then(CommandManager.literal("run").executes(DistributionService::distribute))
        );
    }

    private static int addSpawnPoint(CommandContext<ServerCommandSource> context, String name) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        exec.sendMessage(ChatUtil.newMessage("Added: " + name));
        DataContainer spawnPoint = spawnPoints.createDataContainer(name);
        spawnPoint.put("pos", exec.getBlockPos());
        return 0;
    }

    private static int removeSpawnPoint(CommandContext<ServerCommandSource> context, String name) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        exec.sendMessage(ChatUtil.newMessage("Removed: " + name));
        spawnPoints.drop(name);
        return 0;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        spawnPoints.getIds().forEach(id -> {
            DataContainer container = spawnPoints.get(id);
            if (container == null) return;

            exec.sendMessage(ChatUtil.newMessage(id + " : " + container.getBlockPos("pos")));
        });
        return 0;
    }

    private static int distribute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        ArrayList<UUID> singles = new ArrayList<>();
        Reincarnaria.userCache.load().forEach(entry -> singles.add(entry.getProfile().getId()));

        List<DataContainer> points = spawnPoints.getDataContainers().stream().toList();
        if (points.isEmpty()) {
            ChatUtil.ERROR(exec, "No SpawnPoints Defined - Run /distribution addSpawn <name>");
            return 1;
        }
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

        ArrayList<ArrayList<UUID>> teams = new ArrayList<>(maxTeams);
        for (int i = 0; i < maxTeams; i++) teams.add(new ArrayList<>());

        int index = 0;
        loop:
        if (!parties.isEmpty()) {
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
        }

        if (index == -1) {
            ChatUtil.ERROR(exec, "Failed, sorry bozo");
            return 1;
        }

        index = 0;
        loop:
        for (ArrayList<UUID> team : teams) {
            while (team.size() < maxTeamSize) {
                if (index == singles.size()) break loop;

                team.add(singles.get(index));
                index++;
            }
        }

        index = 0;
        for (ArrayList<UUID> team : teams) {
            DataContainer container = points.get(index);
            index++;

            for (UUID player : team) {
                DataContainer playerData = DistributionService.playerData.get(player);
                if (playerData == null) playerData = DistributionService.playerData.createDataContainer(player);

                playerData.put("teleported", false);
                playerData.put("spawnPoint", container.getBlockPos("pos"));
            }
        }

        exec.sendMessage(ChatUtil.newMessage("Done."));
        return 0;
    }

    public static void init(MinecraftServer mcServer, Database database) {
        registerCommands(mcServer.getCommandFunctionManager().getDispatcher());

        spawnPoints = database.createTable("spawnpoints").addColumn("pos", SQLDataType.BLOCKPOS).finish();
        playerData = database.createTable("playerData")
                .addColumn("partyId", SQLDataType.UUID)
                .addColumn("invites", SQLDataType.JSON)
                .addColumn("spawnPoint", SQLDataType.BLOCKPOS)
                .addColumn("teleported", SQLDataType.BOOL)
                .finish();
        partyDataTable = database.createTable("partyData").addColumn("partyData", SQLDataType.JSON).finish();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            DataContainer playerData = DistributionService.playerData.get(player.getUuid());
            if (playerData == null || playerData.getBool("teleported")) return;

            playerData.put("teleported", true);
            BlockPos pos = playerData.getBlockPos("spawnPoint");
            player.teleport(pos.getX(), pos.getY(), pos.getZ());
        });
    }
}
