package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void disableAttack(Entity target, CallbackInfo ci) {
        if (VanishAPI.isVanished((ServerPlayerEntity) (Object) this)) ci.cancel();
    }

    @Inject(method = "canModifyAt", at = @At("HEAD"), cancellable = true)
    private void disableCanModifyAt(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (VanishAPI.isVanished((ServerPlayerEntity) (Object) this)) cir.setReturnValue(false);
    }
}