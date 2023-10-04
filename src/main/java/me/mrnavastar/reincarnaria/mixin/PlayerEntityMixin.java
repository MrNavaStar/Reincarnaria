package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "shouldCancelInteraction", at = @At("HEAD"), cancellable = true)
    private void cancelInteraction(CallbackInfoReturnable<Boolean> cir) {
        if (VanishAPI.isVanished((PlayerEntity) (Object) this)) cir.setReturnValue(true);
    }
}