package me.mrnavastar.reincarnaria.services.permadeath;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lombok.Getter;
import me.drex.vanish.api.VanishAPI;
import me.mrnavastar.reincarnaria.Reincarnaria;
import me.mrnavastar.reincarnaria.util.ChatUtil;
import me.mrnavastar.reincarnaria.util.HeadUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.concurrent.Executors;

public class PermaDeathService {

    @Getter
    private static boolean enabled = false;

    private static void registerCommands(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
            CommandManager.literal("revive").requires(source -> source.hasPermissionLevel(4))

                .executes(ctx -> {
                    ServerPlayerEntity exec = ctx.getSource().getPlayer();
                    if (exec != null) revivePlayer(ctx, exec);
                    return 1;
                })

                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> revivePlayer(ctx, EntityArgumentType.getPlayer(ctx, "player"))))
        );

        commandDispatcher.register(
                CommandManager.literal("grace").requires(source -> source.hasPermissionLevel(4))

                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> grace(ctx, BoolArgumentType.getBool(ctx, "enabled"))))

        );
    }

    private static int grace(CommandContext<ServerCommandSource> context, boolean b) {
        ServerPlayerEntity exec = context.getSource().getPlayer();
        if (exec == null) return 1;

        enabled = !b;
        if (enabled) Reincarnaria.playerManager.getPlayerList().forEach(player -> player.sendMessage(ChatUtil.newMessage("<hover:show_text:'oh no! anyway...'><i><color:#ffda33><color:#ff922b><color:#ffc933><b>GRACE PERIOD HAS ENDED!</b></color></color></color></i></hover>")));
        return 0;
    }

    private static int revivePlayer(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        if (!VanishAPI.isVanished(player)) {
            MutableText text = (MutableText) Text.of(player.getName().getString() + " is not dead!");
            text.formatted(Formatting.RED);
            context.getSource().sendMessage(text);
            return 1;
        }

        player.changeGameMode(GameMode.SURVIVAL);
        player.clearStatusEffects();
        VanishAPI.setVanish(player, false);

        MutableText text = (MutableText) Text.of(player.getName().getString() + " rose from the dead!");
        text.formatted(Formatting.GREEN);
        context.getSource().sendMessage(text);
        if (context.getSource().getPlayer() != player) {
            player.sendMessage(text);
        }
        return 0;
    }

    public static void init(MinecraftServer server) {
        registerCommands(server.getCommandFunctionManager().getDispatcher());

        // On Player Death
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (!enabled) return true;

                Reincarnaria.playerManager.getPlayerList().forEach(p -> {
                    p.sendMessage(damageSource.getDeathMessage(player));
                });

                player.getServerWorld().spawnParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                player.getServerWorld().spawnParticles(ParticleTypes.SOUL, player.getX(), player.getY(), player.getZ(), 20, 0, 0, 0, 0.08);
                player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 1f, 1f);

                MutableText text = (MutableText) Text.of("YOU DIED");
                text.formatted(Formatting.RED);
                text.formatted(Formatting.BOLD);
                player.networkHandler.sendPacket(new TitleS2CPacket(text));

                player.getInventory().dropAll();
                player.setHealth(20);
                player.getHungerManager().setFoodLevel(20);
                player.getServerWorld().spawnEntity(new ExperienceOrbEntity(player.getServerWorld(), player.getX(), player.getY(), player.getZ(), player.experienceLevel));
                player.setExperiencePoints(0);
                player.setExperienceLevel(0);

                Executors.newSingleThreadExecutor().execute(() -> {
                    ItemStack skull = Items.PLAYER_HEAD.getDefaultStack();
                    NbtCompound compound = HeadUtils.nbtFromProfile(player.getGameProfile());
                    skull.setNbt(compound);
                    if (damageSource.getAttacker() instanceof ServerPlayerEntity attacker) {
                        skull.setNbt(HeadUtils.addLore(compound, attacker));
                    }
                    player.getServerWorld().spawnEntity(new ItemEntity(player.getServerWorld(), player.getX(), player.getY(), player.getZ(), skull));
                });

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