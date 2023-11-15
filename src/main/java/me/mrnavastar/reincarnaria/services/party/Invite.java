package me.mrnavastar.reincarnaria.services.party;

import com.mojang.authlib.GameProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mrnavastar.reincarnaria.Reincarnaria;

import java.util.Optional;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Invite {

    private final UUID invitor;
    private final UUID party;

    public GameProfile getInvitorProfile() {
        Optional<GameProfile> profile = Reincarnaria.userCache.getByUuid(invitor);
        return profile.orElse(null);
    }
}
