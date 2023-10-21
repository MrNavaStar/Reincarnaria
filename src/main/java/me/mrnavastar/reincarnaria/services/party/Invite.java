package me.mrnavastar.reincarnaria.services.party;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import me.mrnavastar.reincarnaria.Reincarnaria;

import java.util.Optional;
import java.util.UUID;

@Getter
public class Invite {

    private final UUID invitor;
    private final UUID party;

    public Invite(UUID invitor, UUID party) {
        this.invitor = invitor;
        this.party = party;
    }

    public GameProfile getInvitorProfile() {
        Optional<GameProfile> profile = Reincarnaria.userCache.getByUuid(invitor);
        return profile.orElse(null);
    }
}
