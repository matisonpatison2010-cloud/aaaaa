package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "macemod";

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            // MAIN HAND ONLY
            if (hand != player.getActiveHand()) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getMainHandStack();

            // Only works with the mace
            if (!stack.isOf(Items.MACE)) {
                return TypedActionResult.pass(stack);
            }

            // --------------------
            // WIND CHARGED
            // Launch player ~8 blocks upward
            // --------------------
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, 1.6, velocity.z);
            player.velocityModified = true;

            world.playSound(
                    null,
                    player.getBlockPos(),
                    SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
            );

            // --------------------
            // ENDER MIST
            // 5 seconds (100 ticks)
            // --------------------
            player.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.INVISIBILITY,
                            100,
                            0,
                            false,
                            false,
                            true
                    )
            );

            world.spawnParticles(
                    ParticleTypes.PORTAL,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    80,
                    0.5,
                    0.5,
                    0.5,
                    0.2
            );

            world.playSound(
                    null,
                    player.getBlockPos(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
            );

            return new TypedActionResult<>(ActionResult.SUCCESS, stack);
        });
    }
}
