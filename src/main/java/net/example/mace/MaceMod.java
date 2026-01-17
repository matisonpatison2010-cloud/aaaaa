package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {
    public static final String MODID = "mace";

    public static Enchantment WIND_CHARGED;

    @Override
    public void onInitialize() {

        // ─── Wind Charged Enchantment ───
        WIND_CHARGED = Registry.register(
                Registries.ENCHANTMENT,
                Identifier.of(MODID, "wind_charged"),
                new Enchantment(
                        Enchantment.Rarity.RARE,
                        EnchantmentTarget.WEAPON,
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND}
                ) {
                    @Override
                    public boolean isAcceptableItem(ItemStack stack) {
                        return stack.isOf(Items.MACE);
                    }
                }
        );

        // ─── Right Click Ability ───
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.hasEnchantments()) return ActionResult.PASS;

            if (stack.getEnchantments().toString().contains("wind_charged")) {

                // Launch player upward
                player.addVelocity(0, 1.2, 0);
                player.velocityModified = true;

                // Play wind charge sound
                world.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                // Spawn particles
                for (int i = 0; i < 40; i++) {
                    Vec3d p = player.getPos().add(
                            world.random.nextGaussian() * 0.3,
                            0.1,
                            world.random.nextGaussian() * 0.3
                    );
                    world.addParticle(
                            ParticleTypes.CLOUD,
                            p.x, p.y, p.z,
                            0, 0.1, 0
                    );
                }

                // Mark player to cancel fall damage ONCE
                player.getPersistentData().putBoolean("wind_charged_jump", true);

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        // ─── Cancel Fall Damage ONLY After Ability ───
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player.getPersistentData().getBoolean("wind_charged_jump")) {
                player.fallDistance = 0;
                player.getPersistentData().remove("wind_charged_jump");
            }
            return true;
        });
    }
}
