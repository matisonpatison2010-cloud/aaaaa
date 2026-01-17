package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    /* ---------------- ENCHANTMENT KEYS ---------------- */

    public static final RegistryKey<Enchantment> WIND_CHARGED =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_charged"));

    public static final RegistryKey<Enchantment> ENDER_MIST =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "ender_mist"));

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);

            /* ---------------- WIND CHARGED ---------------- */
            if (stack.getItem() == Items.MACE) {

                RegistryEntry<Enchantment> wind =
                        world.getRegistryManager()
                                .get(RegistryKeys.ENCHANTMENT)
                                .getEntry(WIND_CHARGED)
                                .orElse(null);

                if (wind != null) {
                    int level = EnchantmentHelper.getLevel(wind, stack);

                    if (level > 0 && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {

                        double launch = 1.0 + (level * 0.6);
                        player.addVelocity(0, launch, 0);
                        player.velocityModified = true;

                        // IMPORTANT: only cancel fall damage CAUSED by the launch
                        player.setOnGround(false);
                        player.fallDistance = 0;

                        world.playSound(
                                null,
                                player.getBlockPos(),
                                SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST,
                                SoundCategory.PLAYERS,
                                1.0f,
                                1.0f
                        );

                        ((World) world).spawnParticles(
                                ParticleTypes.GUST,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                20,
                                0.3, 0.1, 0.3,
                                0.02
                        );

                        player.getItemCooldownManager().set(stack.getItem(), 100);
                        return TypedActionResult.success(stack);
                    }
                }
            }

            /* ---------------- ENDER MIST ---------------- */
            if (stack.isOf(Items.MACE)
                    || stack.isOf(Items.SWORD)
                    || stack.isOf(Items.AXE)
                    || stack.isOf(Items.PICKAXE)
                    || stack.isOf(Items.SHOVEL)
                    || stack.isOf(Items.HOE)) {

                RegistryEntry<Enchantment> mist =
                        world.getRegistryManager()
                                .get(RegistryKeys.ENCHANTMENT)
                                .getEntry(ENDER_MIST)
                                .orElse(null);

                if (mist != null) {
                    int level = EnchantmentHelper.getLevel(mist, stack);

                    if (level > 0 && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {

                        AreaEffectCloudEntity cloud =
                                new AreaEffectCloudEntity(world, player.getX(), player.getY(), player.getZ());

                        cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
                        cloud.setRadius(3.0F);
                        cloud.setDuration((3 + level) * 20); // 3s + 1s per tier
                        cloud.setRadiusGrowth(0);
                        cloud.setOwner(player);
                        cloud.addEffect(StatusEffects.HARM.createStatusEffect(1, 1));

                        world.spawnEntity(cloud);

                        world.playSound(
                                null,
                                player.getBlockPos(),
                                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                                SoundCategory.PLAYERS,
                                1.2f,
                                1.0f
                        );

                        player.getItemCooldownManager().set(stack.getItem(), 400); // 20s
                        return TypedActionResult.success(stack);
                    }
                }
            }

            return TypedActionResult.pass(stack);
        });
    }
}
