package dev.migi.megumin.item;

import dev.migi.megumin.effect.ModEffects;
import dev.migi.megumin.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    // 13 seconds — synced to when Megumin shouts "EXPLOSION!!!" in the audio
    private static final int CHARGE_TICKS = 260;
    // Post-cast exhaustion cooldown: 10 seconds
    private static final int COOLDOWN_TICKS = 200;
    // Anti-spam: audio is 21s, block re-cast for full audio duration
    private static final int AUDIO_COOLDOWN_TICKS = 420;

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

        // Lock the item for full audio duration to prevent spam
        if (!level.isClientSide) {
            player.getCooldowns().addCooldown(this, AUDIO_COOLDOWN_TICKS);
        }

        // Play Megumin's chant — null = everyone including caster hears it
        level.playSound(null,
            player.getX(), player.getY(), player.getZ(),
            ModSounds.EXPLOSION_CHANT.get(),
            SoundSource.PLAYERS, 4.0f, 1.0f
        );

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        if (!level.isClientSide) return;

        int usedTicks = CHARGE_TICKS - remainingUseDuration;
        float progress = usedTicks / (float) CHARGE_TICKS;
        double rot = usedTicks * 0.08; // rotation speed (radians/tick)

        Vec3 pos = player.position();

        // ── GROUND CIRCLES ──────────────────────────────────────────────────
        // Outer ring — expands and rotates clockwise
        double outerR = 2.5 + progress * 2.5;
        for (int i = 0; i < 40; i++) {
            double a = (i / 40.0) * Math.PI * 2 + rot;
            level.addParticle(ParticleTypes.FLAME,
                pos.x + Math.cos(a) * outerR, pos.y + 0.1, pos.z + Math.sin(a) * outerR,
                0, 0.02, 0);
        }

        // Inner ring — counter-rotates
        double innerR = outerR * 0.5;
        for (int i = 0; i < 24; i++) {
            double a = (i / 24.0) * Math.PI * 2 - rot;
            level.addParticle(ParticleTypes.LAVA,
                pos.x + Math.cos(a) * innerR, pos.y + 0.05, pos.z + Math.sin(a) * innerR,
                0, 0, 0);
        }

        // Tiny center ring
        double tinyR = innerR * 0.4;
        for (int i = 0; i < 12; i++) {
            double a = (i / 12.0) * Math.PI * 2 + rot * 2;
            level.addParticle(ParticleTypes.END_ROD,
                pos.x + Math.cos(a) * tinyR, pos.y + 0.15, pos.z + Math.sin(a) * tinyR,
                0, 0.01, 0);
        }

        // ── VERTICAL CIRCLE (perpendicular to look — like the anime) ────────
        Vec3 look = player.getLookAngle().normalize();

        // Find right and up vectors perpendicular to look direction
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = look.cross(worldUp).normalize();
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0); // edge case: looking straight up/down
        Vec3 up = right.cross(look).normalize();

        // Circle center grows further as charge builds
        double dist = 4.0 + progress * 4.0;
        Vec3 center = pos.add(0, 1, 0).add(look.scale(dist));
        double circleR = 1.5 + progress * 2.5;

        // Outer vertical ring — FLAME
        for (int i = 0; i < 36; i++) {
            double a = (i / 36.0) * Math.PI * 2 + rot * 2;
            Vec3 point = center
                .add(right.scale(Math.cos(a) * circleR))
                .add(up.scale(Math.sin(a) * circleR));
            level.addParticle(ParticleTypes.FLAME, point.x, point.y, point.z, 0, 0.01, 0);
        }

        // Inner vertical ring — END_ROD (white glow), counter-rotating
        double innerCircleR = circleR * 0.55;
        for (int i = 0; i < 24; i++) {
            double a = (i / 24.0) * Math.PI * 2 - rot * 3;
            Vec3 point = center
                .add(right.scale(Math.cos(a) * innerCircleR))
                .add(up.scale(Math.sin(a) * innerCircleR));
            level.addParticle(ParticleTypes.END_ROD, point.x, point.y, point.z, 0, 0, 0);
        }

        // ── RISING ENERGY STREAMS from player toward the circle ─────────────
        if (usedTicks % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = rot * 3 + i * (Math.PI / 2);
                double r = 0.6;
                Vec3 src = pos.add(Math.cos(a) * r, 1.2, Math.sin(a) * r);
                Vec3 dir = center.subtract(src).normalize().scale(0.15);
                level.addParticle(ParticleTypes.FLAME,
                    src.x, src.y, src.z,
                    dir.x, dir.y + 0.05, dir.z);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) return;

        if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            // Stop the chant audio immediately
            serverPlayer.connection.send(new ClientboundStopSoundPacket(
                ResourceLocation.fromNamespaceAndPath("megumin", "explosion_chant"),
                SoundSource.PLAYERS
            ));

            // Short penalty cooldown (3 seconds) for interrupting
            player.getCooldowns().addCooldown(this, 60);

            player.sendSystemMessage(
                Component.literal("§7[§dMegumin§7] §eConcentration broken! I need a moment...")
            );
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (level.isClientSide) return stack;

        ServerLevel serverLevel = (ServerLevel) level;

        // === EXPLOSION — fires exactly when Megumin shouts "EXPLOSION!!!" ===
        Vec3 eyePos = player.getEyePosition();
        Vec3 target = eyePos.add(player.getLookAngle().scale(10.0));

        serverLevel.explode(null, target.x, target.y, target.z, 6.0f, Level.ExplosionInteraction.TNT);

        // Particle burst at explosion point
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y, target.z, 5, 2, 2, 2, 0.1);
        serverLevel.sendParticles(ParticleTypes.FLAME,            target.x, target.y, target.z, 100, 3, 3, 3, 0.5);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,      target.x, target.y, target.z, 50, 2, 2, 2, 0.2);

        // Boom sound
        level.playSound(null, player.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0f, 0.8f);

        // === MEGUMIN COLLAPSES ===
        player.addEffect(new MobEffectInstance(ModEffects.EXPLOSION_EXHAUSTION, 200, 1, false, true, true));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200, 1));
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 3));

        player.sendSystemMessage(
            Component.literal("§d§lEXPLOSION!!!! §7(and now I can't move... as expected)")
                .withStyle(ChatFormatting.ITALIC)
        );

        // Reset to post-cast recovery cooldown (10s)
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
