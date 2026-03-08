package dev.migi.megumin.item;

import dev.migi.megumin.effect.ModEffects;
import dev.migi.megumin.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ExplosionStaffItem extends Item {

    // 13 seconds = when Megumin shouts "EXPLOSION!!!" in the audio (synced!)
    private static final int CHARGE_TICKS = 260;
    // 10 seconds post-cast exhaustion cooldown
    private static final int COOLDOWN_TICKS = 200;

    public ExplosionStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(
                    Component.literal("§7[§dMegumin§7] §cI haven't recovered yet! Just wait a bit longer...")
                        .withStyle(ChatFormatting.ITALIC)
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        // Apply cooldown immediately on server — covers full charge + post-cast recovery
        // This prevents audio spam: once the chant starts, you must wait it out
        if (!level.isClientSide) {
            player.getCooldowns().addCooldown(this, CHARGE_TICKS + COOLDOWN_TICKS);
        }

        // Play Megumin's chant — null means everyone including the caster hears it
        level.playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            ModSounds.EXPLOSION_CHANT.get(),
            SoundSource.PLAYERS,
            4.0f, 1.0f
        );

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;

        int usedTicks = CHARGE_TICKS - remainingUseDuration;

        // Particles build up as the charge fills — bigger and denser over time
        if (level.isClientSide) {
            Vec3 pos = player.position().add(0, 1, 0);
            double radius = 0.3 + (usedTicks / (double) CHARGE_TICKS) * 2.0;
            int count = 2 + (int)(usedTicks / (double) CHARGE_TICKS * 6);

            for (int i = 0; i < count; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;
                level.addParticle(ParticleTypes.FLAME,
                    pos.x + dx, pos.y + level.random.nextDouble(), pos.z + dz, 0, 0.05, 0);
                level.addParticle(ParticleTypes.WITCH,
                    pos.x + dx, pos.y + 0.5 + level.random.nextDouble(), pos.z + dz, 0, 0.05, 0);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) return;
        // Released before full charge — cooldown from use() still running, they must wait
        if (!level.isClientSide) {
            player.sendSystemMessage(
                Component.literal("§7[§dMegumin§7] §eI lost my concentration! Don't interrupt me!")
            );
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (level.isClientSide) return stack;

        ServerLevel serverLevel = (ServerLevel) level;

        // === EXPLOSION — fires exactly when she shouts "EXPLOSION!!!" ===
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 target = eyePos.add(lookVec.scale(10.0));

        serverLevel.explode(null, target.x, target.y, target.z, 6.0f, Level.ExplosionInteraction.TNT);

        // Particle burst at explosion point
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y, target.z, 5, 2, 2, 2, 0.1);
        serverLevel.sendParticles(ParticleTypes.FLAME, target.x, target.y, target.z, 100, 3, 3, 3, 0.5);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, target.x, target.y, target.z, 50, 2, 2, 2, 0.2);

        // Explosion boom sound
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0f, 0.8f);

        // === MEGUMIN COLLAPSES ===
        player.addEffect(new MobEffectInstance(ModEffects.EXPLOSION_EXHAUSTION, 200, 1, false, true, true));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200, 1));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 3));

        player.sendSystemMessage(
            Component.literal("§d§lEXPLOSION!!!! §7(and now I can't move... as expected)")
                .withStyle(ChatFormatting.ITALIC)
        );

        // Reset cooldown to just the post-cast recovery period
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
