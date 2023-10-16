package me.mrnavastar.reincarnaria.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.electrobrine.quill_notifications.api.Pigeon;
import me.electrobrine.quill_notifications.api.QuillEvents;
import me.mrnavastar.reincarnaria.Reincarnaria;
import me.mrnavastar.reincarnaria.util.ChatUtil;
import mrnavastar.sqlib.DataContainer;
import mrnavastar.sqlib.Table;
import mrnavastar.sqlib.database.Database;
import mrnavastar.sqlib.sql.SQLDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


public class PartyService {

    private static MinecraftServer server;
    private static Table partyLookupTable;
    private static Table partyDataTable;

    private static void registerCommands(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
            CommandManager.literal("party").requires(ServerCommandSource::isExecutedByPlayer)
                .executes(PartyService::help)
                .then(CommandManager.literal("help").executes(PartyService::help))

                .then(CommandManager.literal("list").executes(PartyService::listPlayers))

                .then(CommandManager.literal("invite")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .executes(ctx -> invitePlayer(ctx, GameProfileArgumentType.getProfileArgument(ctx, "player")))
                    )
                )

                .then(CommandManager.literal("accept")
                    /*.then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .executes(ctx -> {
                                Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(ctx, "player");
                                gameProfiles.forEach(profile -> acceptInvite(ctx, profile.getId()));
                                return 0;
                            })
                    )*/

                    .then(CommandManager.argument("uuid", StringArgumentType.string())
                            .executes(ctx -> {
                                try {
                                    UUID uuid = UUID.fromString(StringArgumentType.getString(ctx, "uuid"));
                                    acceptInvite(ctx, uuid);
                                } catch (Exception ignore) {}
                                return 0;
                            })
                    )
                )

                .then(CommandManager.literal("pending").executes(PartyService::pending))

                .then(CommandManager.literal("leave").executes(PartyService::leave))
        );
    }

    private static int help(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        exec.sendMessage(ChatUtil.newMessage("The party system keeps you and your friends together when the game starts.\nTo learn more, join our <hover:show_text:'<color:#7289da>join now!</color>'><click:open_url:'https://discord.gg/2MUxFaaekP'><color:#7289da>discord</color></click></hover>, or read the <color:#b3816d><hover:show_text:'<color:#b3816d>check your inventory</color>'>book</hover></color>."));
        return 0;
    }

    private static int listPlayers(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        Party party = lookupParty(exec.getUuid());
        if (party == null) {
            ChatUtil.ERROR(exec, "You are not in a party!");
            return 1;
        }

        exec.sendMessage(ChatUtil.newMessage("<b><color:#b5b5b5>Party Players -----</color></b>"));
        party.getMembers().forEach(member -> {
            Reincarnaria.userCache.getByUuid(member).ifPresent(gameProfile -> {
                String out = gameProfile.getName();
                if (member.equals(party.getLeader())) out += " <i>(leader)</i>: ";
                else out += ": ";

                if (Reincarnaria.playerManager.getPlayer(member) != null) out += "<color:#5ec9c1>Online</color>";
                else out += "<color:#b5b5b5>Offline</color>";
                exec.sendMessage(ChatUtil.newMessage(out));
            });
        });

        if (party.getInviteNames().isEmpty()) return 1;
        exec.sendMessage(ChatUtil.newMessage("<b><color:#b5b5b5>Invited Players ----</color></b>"));
        party.getInviteNames().forEach(invite -> exec.sendMessage(ChatUtil.newMessage(invite + ": <color:#dbae1c>Pending</color>")));
        return 0;
    }

    private static int invitePlayer(CommandContext<ServerCommandSource> context, Collection<GameProfile> profiles) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null || profiles.size() > 1) return 1;

        System.out.println(exec.getUuid());
        Party party = lookupParty(exec.getUuid());
        System.out.println("here");
        if (party == null) {
            party = new Party(exec.getUuid());
        }

        for (GameProfile profile : profiles) {
            if (profile.getId() == exec.getUuid()) {
                ChatUtil.ERROR(exec, "You cannot invite yourself to your own party!");
                continue;
            }

            if (party.getInvitesUuids().contains(profile.getId())) {
                ChatUtil.ERROR(exec, "You have already invited " + profile.getName() + "!");
                continue;
            }

            if (lookupParty(profile.getId()) != null) {
                ChatUtil.ERROR(exec, "That player is already in a party!");
                continue;
            }

            party.invite(profile.getName(), profile.getId());
            addToPartyLookup(exec.getUuid(), party);
            addInviteLookup(profile.getId(), exec.getUuid(), party);
            savePartyData(party);

            exec.sendMessage(ChatUtil.newMessage("Invite sent to <color:#d695ff>" + profile.getName() + "</color>!"));
            Component message = ChatUtil.newMessage("<i><color:#d695ff><source></color></i> sent you a party invite!\n<color:#00d49e>-> <click:run_command:'/party accept " + profile.getId() + "'><hover:show_text:'/party accept " + profile.getId() + "'><u>CLICK TO ACCEPT</u></hover></click> <-</color>");
            JsonObject meta = new JsonObject();
            meta.addProperty("source", exec.getUuid().toString());
            Pigeon.send(profile.getId(), message, meta, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
        }
        return 0;
    }

    private static void acceptInvite(CommandContext<ServerCommandSource> context, UUID player) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return;

        Party party = lookupParty(player);
        if (party == null) {
            ChatUtil.ERROR(exec, "No Invite found from that player!");
            return;
        }

        if (party.accept(exec.getUuid())) {
            addToPartyLookup(exec.getUuid(), party);
            savePartyData(party);
            Reincarnaria.userCache.getByUuid(player).ifPresent(gameProfile -> {
                exec.sendMessage(ChatUtil.newMessage("You are now partied with <color:#d695ff>" + gameProfile.getName() + "</color>!\n<color:#79baff><click:run_command:'/party list'><hover:show_text:'/party list'>Check it out!</hover></click></color>"));
                Component message = ChatUtil.newMessage("<color:#d695ff><source></color> accepted your invite!");
                JsonObject meta = new JsonObject();
                meta.addProperty("source", exec.getUuid().toString());
                Pigeon.send(gameProfile.getId(), message, meta, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
            });
            return;
        }
        ChatUtil.ERROR(exec, "No Invite found from that player!");
    }

    private static int pending(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        ArrayList<Invite> invites = getInvites(exec.getUuid());
        if (invites == null) return 1;

        exec.sendMessage(ChatUtil.newMessage("<b><color:#b5b5b5>Pending Invites ----</color></b>"));
        invites.forEach(invite -> {
            Party party = getParty(invite.getParty());
            if (party == null) return;

            GameProfile invitor = invite.getInvitorProfile();
            if (invitor == null) return;

            exec.sendMessage(ChatUtil.newMessage(invitor.getName() + "    <color:#00d49e><click:run_command:'/party accept " + invite.getInvitor() + "'>ACCEPT</click></color>"));
        });
        return 0;
    }

    private static int leave(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        Party party = lookupParty(exec.getUuid());
        if (party == null) {
            ChatUtil.ERROR(exec, "You aren't in a party!");
            return 1;
        }

        party.leave(exec.getUuid());
        partyLookupTable.drop(exec.getUuid());
        Reincarnaria.userCache.getByUuid(party.getLeader()).ifPresent(gameProfile -> exec.sendMessage(ChatUtil.newMessage("You left <color:#d695ff>" + gameProfile.getName() + "'s</color> party.")));

        if (exec.getUuid() == party.getLeader() && !party.getMembers().isEmpty()) {
            party.setLeader(party.getMembers().get(0));
        }
        savePartyData(party);
        return 0;
    }

    private static Party getParty(UUID id) {
        DataContainer partyData = partyDataTable.get(id);
        if (partyData == null) return null;

        return Reincarnaria.GSON.fromJson(partyData.getJson("partyData"), Party.class);
    }

    private static Party lookupParty(UUID player) {
        DataContainer playerData = partyLookupTable.get(player);
        if (playerData == null) return null;

        return getParty(playerData.getUUID("partyId"));
    }

    private static void addToPartyLookup(UUID player, Party party) {
        DataContainer playerData = partyLookupTable.get(player);
        if (playerData == null) playerData = partyLookupTable.createDataContainer(player);

        playerData.put("partyId", party.getId());
    }

    private static void addInviteLookup(UUID player, UUID invitor, Party party) {
        DataContainer playerData = partyLookupTable.get(player);
        if (playerData == null) playerData = partyLookupTable.createDataContainer(player);

        JsonArray invites = (JsonArray) playerData.getJson("invites");
        if (invites == null) invites = new JsonArray();

        invites.add(Reincarnaria.GSON.toJsonTree(new Invite(invitor, party.getId())));
        playerData.put("invites", invites);
    }

    private static ArrayList<Invite> getInvites(UUID player) {
        DataContainer playerData = partyLookupTable.get(player);
        if (playerData == null) return null;

        JsonArray invitesData = (JsonArray) playerData.getJson("invites");
        if (invitesData == null) return null;

        ArrayList<Invite> invites = new ArrayList<>();
        invitesData.forEach(invite -> invites.add(Reincarnaria.GSON.fromJson(invite, Invite.class)));
        return invites;
    }

    private static void savePartyData(Party party) {
        DataContainer partyData = partyDataTable.get(party.getId());
        if (partyData == null) partyData = partyDataTable.createDataContainer(party.getId());

        partyData.put("partyData", Reincarnaria.GSON.toJsonTree(party));
    }

    public static void init(MinecraftServer mcServer, Database database) {
        server = mcServer;
        registerCommands(server.getCommandFunctionManager().getDispatcher());

        partyLookupTable = database.createTable("partyLookup").addColumn("partyId", SQLDataType.UUID).addColumn("invites", SQLDataType.JSON).finish();
        partyDataTable = database.createTable("partyData").addColumn("partyData", SQLDataType.JSON).finish();

        QuillEvents.PRE_SEND_NOTIFICATION.register((message) -> {
            String source = message.getMetadata().getAsJsonObject().get("source").getAsString();
            Optional<GameProfile> gameProfile = Reincarnaria.userCache.getByUuid(UUID.fromString(source));
            if (gameProfile.isEmpty()) return false;

            Component component = message.getComponent();
            TextReplacementConfig config = TextReplacementConfig.builder().match("<source>").replacement(gameProfile.get().getName()).build();
            message.setComponent(component.replaceText(config));
            return true;
        });
    }
}