package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EggEntity.class)
public class EggEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void cancelCollision(HitResult hitResult, CallbackInfo ci) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            if (entityHitResult.getEntity() instanceof ServerPlayerEntity player) {
                if (VanishAPI.isVanished(player)) ci.cancel();
            }
        }
    }
}
