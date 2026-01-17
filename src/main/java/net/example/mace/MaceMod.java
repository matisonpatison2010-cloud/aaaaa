package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "macemod";

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            if (hand != Hand.MAIN_HAND) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.hasNbt()) return TypedActionResult.pass(stack);

            NbtCompound macemod = stack.getNbt().getCompound(MODID);
            if (macemod.isEmpty()) return TypedActionResult.pass(stack);

            boolean hasWind = macemod.contains("wind_charged");
            boolean hasMist = macemod.contains("ender_mist");

            // Incompatible
            if (hasWind && hasMist) return TypedActionResult.fail(stack);

            /* ================= WIND CHARGED ================= */
            if (hasWind && stack.isOf(Items.MACE)) {
                int level = macemod.getInt("wind_charged");
                if (level <= 0) return TypedActionResult.pass(stack);

                if (player.getItemCooldownManager().isCoolingDown(stack.getItem()))
                    return TypedActionResult.fail(stack);

                double boost = 1.0 + (level * 0.6);
                player.addVelocity(0, boost, 0);
                player.velocityModified = true;
                player.fallDistance = 0;

                world.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_WIND_CHARGE_THROW,
                        SoundCategory.PLAYERS,
                        1f,
                        1f
                );

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                            ParticleTypes.GUST,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            20,
                            0.2,
                            0.2,
                            0.2,
                            0.01
                    );
                }

                player.getItemCooldownManager().set(stack.getItem(), 100);
                return TypedActionResult.success(stack);
            }

            /* ================= ENDER MIST ================= */
            if (hasMist && isTool(stack.getItem())) {
                int level = macemod.getInt("ender_mist");
                if (level <= 0) return TypedActionResult.pass(stack);

                if (player.getItemCooldownManager().isCoolingDown(stack.getItem()))
                    return TypedActionResult.fail(stack);

                if (world instanceof ServerWorld serverWorld) {
                    AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
                            world,
                            player.getX(),
                            player.getY(),
                            player.getZ()
                    );

                    cloud.setOwner(player);
                    cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
                    cloud.setRadius(3.0f);
                    cloud.setDuration((3 + level) * 20);
                    cloud.setRadiusGrowth(-0.01f);

                    cloud.addEffect(new StatusEffectInstance(
                            StatusEffects.HARM,
                            1,
                            1
                    ));

                    serverWorld.spawnEntity(cloud);

                    world.playSound(
                            null,
                            player.getBlockPos(),
                            SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                            SoundCategory.PLAYERS,
                            1f,
                            1f
                    );
                }

                player.getItemCooldownManager().set(stack.getItem(), 400);
                return TypedActionResult.success(stack);
            }

            return TypedActionResult.pass(stack);
        });
    }

    private static boolean isTool(Item item) {
        return item == Items.MACE
                || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
                || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE
                || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE
                || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL
                || item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE;
    }
}
