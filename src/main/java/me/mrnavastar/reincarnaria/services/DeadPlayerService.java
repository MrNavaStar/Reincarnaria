package me.mrnavastar.reincarnaria.services;

import me.drex.vanish.api.VanishAPI;
import me.mrnavastar.reincarnaria.Reincarnaria;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class DeadPlayerService {

    public static void init() {

        // On Player Death
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {

                Reincarnaria.playerManager.getPlayerList().forEach(p -> {
                    p.sendMessage(damageSource.getDeathMessage(player));
                });

                ParticleEffect particleEffect = ParticleTypes.SONIC_BOOM;
                ParticleEffect particleEffect2 = ParticleTypes.SOUL;
                player.getServerWorld().spawnParticles(particleEffect, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                player.getServerWorld().spawnParticles(particleEffect2, player.getX(), player.getY(), player.getZ(), 20, 0, 0, 0, 0.08);
                player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 1f, 1f);

                MutableText text = (MutableText) Text.of("YOU DIED");
                text.formatted(Formatting.RED);
                text.formatted(Formatting.BOLD);
                player.networkHandler.sendPacket(new TitleS2CPacket(text));

                player.getInventory().dropAll();
                player.setHealth(20);
                player.getHungerManager().setFoodLevel(20);
                player.getServerWorld().spawnEntity(new ExperienceOrbEntity(player.getServerWorld(), player.getX(), player.getY(), player.getZ(), player.getXpToDrop()));
                player.setExperiencePoints(0);
                player.setExperienceLevel(0);

                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 10, true, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 0, true, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 99, true, false));

                player.changeGameMode(GameMode.ADVENTURE);
                VanishAPI.setVanish(player, true);
                return false;
            }
            return true;
        });
    }
}
