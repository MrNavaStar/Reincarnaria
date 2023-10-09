package me.mrnavastar.reincarnaria.services;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.mrnavastar.reincarnaria.Reincarnaria;
import mrnavastar.sqlib.DataContainer;
import mrnavastar.sqlib.Table;
import mrnavastar.sqlib.database.Database;
import mrnavastar.sqlib.sql.SQLDataType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;

import java.util.Collection;
import java.util.UUID;


public class PartyService {

    private static MinecraftServer server;
    private static Table partyLookupTable;
    private static Table partyDataTable;

    private static void registerCommands(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
            CommandManager.literal("party").requires(ServerCommandSource::isExecutedByPlayer)

                .then(CommandManager.literal("list").executes(PartyService::listPlayers))

                .then(CommandManager.literal("invite")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .executes(ctx -> invitePlayer(ctx, GameProfileArgumentType.getProfileArgument(ctx, "player")))
                    )
                )

                .then(CommandManager.literal("accept")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .executes(ctx -> acceptInvite(ctx, GameProfileArgumentType.getProfileArgument(ctx, "player")))
                    )
                )

                .then(CommandManager.literal("leave").executes(PartyService::leave))
        );
    }

    private static int listPlayers(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        Party party = lookupParty(exec.getUuid());
        if (party == null) return 1;

        exec.sendMessage(Text.of("Party Players ----------"));

        PlayerManager playerManager = server.getPlayerManager();
        UserCache userCache = server.getUserCache();
        if (userCache == null) return 1;

        party.getMembers().forEach(member -> {
            userCache.getByUuid(member).ifPresent(gameProfile -> {
                String out = gameProfile.getName();
                if (member.equals(party.getLeader())) out += " (Leader): ";
                else out += ": ";

                if (playerManager.getPlayer(member) != null) out += "Online";
                else out += "Offline";
                exec.sendMessage(Text.of(out));
            });
        });

        exec.sendMessage(Text.of(""));
        exec.sendMessage(Text.of("Invited Players ---------"));

        party.getInviteNames().forEach(invite -> {
            exec.sendMessage(Text.of(invite + ": Pending"));
        });

        return 0;
    }

    private static int invitePlayer(CommandContext<ServerCommandSource> context, Collection<GameProfile> profiles) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null || profiles.size() > 1) return 1;

        Party party = lookupParty(exec.getUuid());
        if (party == null) {
            party = new Party(exec.getUuid());
        }

        for (GameProfile profile : profiles) {
            if (profile.getId() == exec.getUuid()) {
                exec.sendMessage(Text.of("You cannot invite yourself to your own party!"));
                continue;
            }

            if (lookupParty(profile.getId()) != null) {
                exec.sendMessage(Text.of("That player is already in a party!"));
                continue;
            }

            party.invite(profile.getName(), profile.getId());
            addToPartyLookup(exec.getUuid(), party);
            savePartyData(party);
            exec.sendMessage(Text.of("Invited: " + profile.getName() + " to your party!"));
        }
        return 0;
    }

    private static int acceptInvite(CommandContext<ServerCommandSource> context, Collection<GameProfile> profiles) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null || profiles.size() > 1) return 1;

        UserCache userCache = server.getUserCache();
        if (userCache == null) return 1;

        profiles.forEach(profile -> {
            Party party = lookupParty(profile.getId());
            if (party == null) {
                exec.sendMessage(Text.of("No Invite found from that player!"));
                return;
            }

            if (party.accept(exec.getUuid())) {
                addToPartyLookup(exec.getUuid(), party);
                savePartyData(party);
                userCache.getByUuid(profile.getId()).ifPresent(gameProfile -> exec.sendMessage(Text.of("Accepted " + gameProfile.getName() + "'s Invite!")));
                return;
            }
            exec.sendMessage(Text.of("No Invite found from that player!"));
        });

        return 0;
    }

    private static int leave(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        UserCache userCache = server.getUserCache();
        if (userCache == null) return 1;

        Party party = lookupParty(exec.getUuid());
        if (party == null) {
            exec.sendMessage(Text.of("You aren't in a party!"));
            return 1;
        }

        party.leave(exec.getUuid());
        partyLookupTable.drop(exec.getUuid());
        userCache.getByUuid(exec.getUuid()).ifPresent(gameProfile -> exec.sendMessage(Text.of("You left " + gameProfile.getName() + "'s party.")));

        if (exec.getUuid() == party.getLeader() && !party.getMembers().isEmpty()) {
            party.setLeader(party.getMembers().get(0));
        }
        savePartyData(party);
        return 0;
    }

    private static Party lookupParty(UUID player) {
        DataContainer playerData = partyLookupTable.get(player);
        if (playerData == null) return null;

        UUID partyId = playerData.getUUID("partyId");
        DataContainer partyData = partyDataTable.get(partyId);
        if (partyData == null) return null;

        return Reincarnaria.GSON.fromJson(partyData.getJson("partyData"), Party.class);
    }

    private static void addToPartyLookup(UUID player, Party party) {
        DataContainer playerData = partyLookupTable.createDataContainer(player);
        playerData.put("partyId", party.getPartyId());
    }

    private static void savePartyData(Party party) {
        DataContainer partyData = partyDataTable.get(party.getPartyId());
        if (partyData == null) partyData = partyDataTable.createDataContainer(party.getPartyId());

        partyData.put("partyData", Reincarnaria.GSON.toJsonTree(party));
    }

    public static void init(MinecraftServer mcServer, Database database) {
        server = mcServer;
        registerCommands(server.getCommandFunctionManager().getDispatcher());

        partyLookupTable = database.createTable("partyLookup").addColumn("partyId", SQLDataType.UUID).finish();
        partyDataTable = database.createTable("partyData").addColumn("partyData", SQLDataType.JSON).finish();
    }
}