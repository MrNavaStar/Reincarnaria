package me.mrnavastar.reincarnaria.mixin;

import me.drex.vanish.api.VanishAPI;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public class ArmorStandEntityMixin {

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void disableInteraction(PlayerEntity player, Vec3d hitPos, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (VanishAPI.isVanished(player)) cir.setReturnValue(ActionResult.PASS);
    }
}