package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
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
            if (world.isClient()) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            // ONLY MAIN HAND
            if (hand != player.getActiveHand()) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getMainHandStack();

            // ----------------------------
            // WIND CHARGED (launch up 8 blocks)
            // ----------------------------
            if (stack.hasCustomName()
                    && stack.getName().getString().contains("Wind Charged")) {

                Vec3d velocity = player.getVelocity();
                player.setVelocity(velocity.x, 1.1D, velocity.z);
                player.velocityModified = true;

                world.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_WIND_CHARGE_THROW,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                return TypedActionResult.success(stack);
            }

            // ----------------------------
            // ENDER MIST (5 seconds)
            // ----------------------------
            if (stack.hasCustomName()
                    && stack.getName().getString().contains("Ender Mist")) {

                if (world instanceof ServerWorld serverWorld) {

                    serverWorld.spawnParticles(
                            ParticleTypes.DRAGON_BREATH,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            300,
                            1.5,
                            0.5,
                            1.5,
                            0.02
                    );
                }

                world.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                // poison nearby entities (not yourself)
                world.getOtherEntities(player, player.getBoundingBox().expand(3), e -> e instanceof PlayerEntity)
                        .forEach(entity -> {
                            ((PlayerEntity) entity).addStatusEffect(
                                    new StatusEffectInstance(StatusEffects.POISON, 100, 0)
                            );
                        });

                return TypedActionResult.success(stack);
            }

            return TypedActionResult.pass(stack);
        });
    }
}
