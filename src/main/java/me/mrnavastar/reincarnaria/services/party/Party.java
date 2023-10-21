package me.mrnavastar.reincarnaria.services.party;

import lombok.Getter;

import java.util.ArrayList;
import java.util.UUID;

@Getter
public class Party {

    private final UUID id = UUID.randomUUID();
    private UUID leader;
    private final ArrayList<UUID> members = new ArrayList<>();
    private final ArrayList<UUID> invitesUuids = new ArrayList<>();
    private final ArrayList<String> inviteNames = new ArrayList<>();

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public void invite(String name, UUID uuid) {
        inviteNames.add(name);
        invitesUuids.add(uuid);
    }

    public boolean removeInvite(UUID uuid) {
        System.out.println(uuid);
        int index = invitesUuids.indexOf(uuid);
        if (index == -1) return false;

        invitesUuids.remove(index);
        inviteNames.remove(index);
        return true;
    }

    public boolean accept(UUID uuid) {
        if (!removeInvite(uuid)) return false;
        members.add(uuid);
        return true;
    }

    public void leave(UUID uuid) {
        members.remove(uuid);
        if (uuid.equals(leader) && !isEmpty()) leader = members.get(0);
    }

    public int getSize() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}