package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleInventory.class)
public interface VehicleInventoryMixin {

    @Inject(method = "open", at = @At("HEAD"), cancellable = true)
    private void cancelOpen(PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        if (VanishAPI.isVanished(player)) cir.setReturnValue(ActionResult.PASS);
    }
}