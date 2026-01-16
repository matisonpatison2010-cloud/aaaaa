package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    // Registry key for Wind Charged enchantment (defined in JSON)
    public static final RegistryKey<Enchantment> WIND_CHARGED_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_charged"));

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getStackInHand(hand);

            // Only works with a mace
            if (stack.getItem() != Items.MACE) {
                return TypedActionResult.pass(stack);
            }

            // Get enchantment entry
            RegistryEntry<Enchantment> entry =
                    player.getWorld()
                            .getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .getEntry(WIND_CHARGED_KEY)
                            .orElse(null);

            if (entry == null) {
                return TypedActionResult.pass(stack);
            }

            int level = EnchantmentHelper.getLevel(entry, stack);
            if (level <= 0) {
                return TypedActionResult.pass(stack);
            }

            // 5 second cooldown (100 ticks)
            if (player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                return TypedActionResult.fail(stack);
            }

            // Launch strength:
            // Level I ≈ 5 blocks
            // +3 blocks per level
            double launchVelocity = 1.0 + (level * 0.6);
            player.addVelocity(0, launchVelocity, 0);
            player.velocityModified = true;

            // Prevent fall damage
            player.fallDistance = 0;

            // Wind charge burst sound (FIXED)
            player.playSound(
                    SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST,
                    1.2f,
                    1.0f
            );

            // Wind charge gust particles
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.GUST,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        25,            // particle count
                        0.5, 0.2, 0.5, // spread
                        0.12           // speed
                );
            }

            // Apply cooldown
            player.getItemCooldownManager().set(stack.getItem(), 100);

            return TypedActionResult.success(stack);
        });
    }
}
