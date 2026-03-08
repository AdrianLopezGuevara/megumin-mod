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

            // Darkness effect during the entire charge — sky goes dark like the anime
            player.addEffect(new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS,
                CHARGE_TICKS + 20, 1, false, false, false
            ));
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
        // Don't show effect until 1 second in — let player see their target first
        if (usedTicks < 20) return;

        float progress = (usedTicks - 20) / (float)(CHARGE_TICKS - 20);
        double rot = usedTicks * 0.05;

        Vec3 pos = player.position();

        // Look direction — used to find the target point ahead of the player
        Vec3 look = player.getLookAngle().normalize();

        // ── SKY RINGS — horizontal halos floating ABOVE the target point ─────
        // The rings are flat (parallel to the ground), hovering above where the
        // explosion will land — like the anime halos seen from below/the side
        double skyDist = 6.0 + progress * 10.0;
        Vec3 target = pos.add(0, 1.0, 0).add(look.scale(skyDist));

        // Rings hover above the target, rising higher as charge builds
        double baseHeight = target.y + 1.5 + progress * 3.0;

        // 5 horizontal concentric rings — all flat (XZ plane), stacked slightly
        double maxR = 1.0 + progress * 5.5;
        double[] ringScale = {1.0, 0.75, 0.55, 0.38, 0.22};
        double[] ringSpeed = {1.0, -1.3, 1.6, -2.0, 2.5}; // alternating rotations
        double[] heightOffset = {0, 0.4, 0.8, 1.1, 1.3};  // slightly stacked

        for (int r = 0; r < ringScale.length; r++) {
            double radius = maxR * ringScale[r];
            if (radius < 0.3) continue; // skip tiny rings early in charge
            double ringRot = rot * ringSpeed[r];
            double ringY = baseHeight + heightOffset[r];
            int points = Math.max(24, (int)(radius * 10));

            for (int i = 0; i < points; i++) {
                double a = (i / (double) points) * Math.PI * 2 + ringRot;
                // Flat ring in the XZ plane — horizontal halo
                double rx = target.x + Math.cos(a) * radius;
                double rz = target.z + Math.sin(a) * radius;
                level.addParticle(ParticleTypes.FLAME,   rx, ringY,        rz, 0, 0, 0);
                level.addParticle(ParticleTypes.END_ROD, rx, ringY + 0.12, rz, 0, 0, 0);
            }
        }

        // ── GROUND MAGIC CIRCLE — geometric rune style ───────────────────────
        // Only every 2 ticks to keep it from overpowering
        if (usedTicks % 2 == 0) {
            double outerR = 3.5 + progress * 1.5;
            double y = pos.y + 0.05;

            // Outer crisp circle
            int outerPoints = 48;
            for (int i = 0; i < outerPoints; i++) {
                double a = (i / (double) outerPoints) * Math.PI * 2 + rot * 0.3;
                level.addParticle(ParticleTypes.END_ROD,
                    pos.x + Math.cos(a) * outerR, y, pos.z + Math.sin(a) * outerR, 0, 0, 0);
            }

            // Inner circle (counter-rotating)
            double innerR = outerR * 0.6;
            for (int i = 0; i < 36; i++) {
                double a = (i / 36.0) * Math.PI * 2 - rot * 0.5;
                level.addParticle(ParticleTypes.END_ROD,
                    pos.x + Math.cos(a) * innerR, y, pos.z + Math.sin(a) * innerR, 0, 0, 0);
            }

            // Pentagon inscribed (crisp lines between vertices)
            double pentR = outerR * 0.75;
            for (int i = 0; i < 5; i++) {
                double a1 = (i / 5.0) * Math.PI * 2 - rot * 0.2;
                double a2 = ((i + 1) / 5.0) * Math.PI * 2 - rot * 0.2;
                drawLine(level,
                    pos.x + Math.cos(a1) * pentR, y, pos.z + Math.sin(a1) * pentR,
                    pos.x + Math.cos(a2) * pentR, y, pos.z + Math.sin(a2) * pentR,
                    12, ParticleTypes.FLAME);
            }

            // Pentagram star (skip-one vertices = star pattern)
            double starR = outerR * 0.45;
            for (int i = 0; i < 5; i++) {
                double a1 = (i / 5.0) * Math.PI * 2 + rot * 0.4;
                double a2 = ((i + 2) / 5.0) * Math.PI * 2 + rot * 0.4;
                drawLine(level,
                    pos.x + Math.cos(a1) * starR, y, pos.z + Math.sin(a1) * starR,
                    pos.x + Math.cos(a2) * starR, y, pos.z + Math.sin(a2) * starR,
                    10, ParticleTypes.FLAME);
            }
        }
    }

    /** Draws a line of particles between two points */
    private void drawLine(Level level, double x1, double y1, double z1,
                          double x2, double y2, double z2,
                          int steps, net.minecraft.core.particles.SimpleParticleType type) {
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            level.addParticle(type,
                x1 + t * (x2 - x1), y1 + t * (y2 - y1), z1 + t * (z2 - z1),
                0, 0, 0);
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

            // Remove darkness effect — world returns to normal
            player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);

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

        // Visual-only lightning bolt at explosion point (cosmetic, no damage)
        net.minecraft.world.entity.LightningBolt lightning =
            new net.minecraft.world.entity.LightningBolt(
                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, serverLevel);
        lightning.moveTo(target.x, target.y, target.z);
        lightning.setVisualOnly(true);
        serverLevel.addFreshEntity(lightning);

        // Boom sound
        level.playSound(null, player.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0f, 0.8f);

        // Remove darkness — world snaps back to normal after the explosion
        player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);

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
