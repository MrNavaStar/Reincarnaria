package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatEntity.class)
public class BoatEntityMixin {

    @Inject(method = "canCollide", at = @At("HEAD"), cancellable = true)
    private static void stopCollision(Entity entity, Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof PlayerEntity player) {
            if (VanishAPI.isVanished(player)) cir.setReturnValue(false);
        }
    }
}