package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "canPlayerModifyAt", at = @At("HEAD"), cancellable = true)
    private void disableCanModifyAt(PlayerEntity player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (VanishAPI.isVanished(player)) cir.setReturnValue(false);
    }
}