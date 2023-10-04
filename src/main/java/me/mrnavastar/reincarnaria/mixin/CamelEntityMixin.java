package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CamelEntity.class)
public class CamelEntityMixin {

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void disableMobInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (VanishAPI.isVanished(player)) cir.setReturnValue(ActionResult.PASS);
    }
}