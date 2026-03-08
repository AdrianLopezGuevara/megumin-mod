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

    // Cooldown in ticks (20 ticks = 1 second). 200 = 10 seconds
    private static final int COOLDOWN_TICKS = 200;
    // How long to hold right-click before casting (ticks)
    private static final int CHARGE_TICKS = 60;

    // Whether the chant audio has started this use session
    private boolean chantStarted = false;

    public ExplosionStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(
                    Component.literal("§7[§dMegumin§7] §cI haven't recovered yet! Just wait a bit longer...").withStyle(ChatFormatting.ITALIC)
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        // Play Megumin's chant audio — null = everyone including the caster hears it
        level.playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            ModSounds.EXPLOSION_CHANT.get(),
            SoundSource.PLAYERS,
            4.0f,  // volume
            1.0f   // pitch
        );

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;

        int usedTicks = CHARGE_TICKS - remainingUseDuration;

        // Particle buildup — grows as charge builds up
        if (level.isClientSide) {
            Vec3 pos = player.position().add(0, 1, 0);
            double radius = 0.5 + (usedTicks / (double) CHARGE_TICKS) * 1.5;

            for (int i = 0; i < 3; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;
                level.addParticle(ParticleTypes.FLAME, pos.x + dx, pos.y + level.random.nextDouble(), pos.z + dz, 0, 0.05, 0);
                level.addParticle(ParticleTypes.WITCH, pos.x + dx, pos.y + 0.5 + level.random.nextDouble(), pos.z + dz, 0, 0.05, 0);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) return;
        int usedTicks = CHARGE_TICKS - timeCharged;
        if (usedTicks < CHARGE_TICKS && !level.isClientSide) {
            player.sendSystemMessage(Component.literal("§7[§dMegumin§7] §eCharge interrupted! I need to concentrate!"));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (level.isClientSide) return stack;

        ServerLevel serverLevel = (ServerLevel) level;

        // === EXPLOSION ===
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double distance = 10.0;
        Vec3 target = eyePos.add(lookVec.scale(distance));

        // Giant explosion
        serverLevel.explode(null, target.x, target.y, target.z, 6.0f, Level.ExplosionInteraction.TNT);

        // Extra particles at explosion center
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y, target.z, 5, 2, 2, 2, 0.1);
        serverLevel.sendParticles(ParticleTypes.FLAME, target.x, target.y, target.z, 100, 3, 3, 3, 0.5);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, target.x, target.y, target.z, 50, 2, 2, 2, 0.2);

        // Explosion boom
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0f, 0.8f);

        // === EXHAUSTION EFFECT (Megumin collapses!) ===
        player.addEffect(new MobEffectInstance(ModEffects.EXPLOSION_EXHAUSTION, 200, 1, false, true, true));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200, 1));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 3));

        // Chat feedback
        player.sendSystemMessage(Component.literal("§d§lEXPLOSION!!!! §7(and now I can't move... as expected)").withStyle(ChatFormatting.ITALIC));

        // Cooldown
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
