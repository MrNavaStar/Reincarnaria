package me.mrnavastar.reincarnaria.services;

import java.util.ArrayList;
import java.util.UUID;

public class Party {

    private final UUID partyId = UUID.randomUUID();
    private UUID leader;
    private final ArrayList<UUID> members = new ArrayList<>();
    private final ArrayList<UUID> invitesUuids = new ArrayList<>();
    private final ArrayList<String> inviteNames = new ArrayList<>();

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public void invite(String name, UUID uuid) {
        inviteNames.add(name);
        invitesUuids.add(uuid);
    }

    public boolean accept(UUID uuid) {
        int index = invitesUuids.indexOf(uuid);
        if (index == -1) return false;

        invitesUuids.remove(index);
        inviteNames.remove(index);
        members.add(uuid);
        return true;
    }

    public void leave(UUID uuid) {
        members.remove(uuid);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getLeader() {
        return leader;
    }

    public ArrayList<UUID> getMembers() {
        return members;
    }

    public ArrayList<UUID> getInvitesUuids() {
        return invitesUuids;
    }

    public ArrayList<String> getInviteNames() {
        return inviteNames;
    }
}