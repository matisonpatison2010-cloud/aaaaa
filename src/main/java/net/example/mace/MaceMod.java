package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    public static Enchantment WIND_CHARGED;

    public static final RegistryKey<Enchantment> WIND_CHARGED_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_charged"));

    @Override
    public void onInitialize() {

        // Register Wind Charged enchantment (MACE ONLY)
        WIND_CHARGED = Registry.register(
                Registries.ENCHANTMENT,
                Identifier.of(MODID, "wind_charged"),
                new Enchantment(
                        Enchantment.Rarity.RARE,
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND}
                ) {
                    @Override
                    public boolean isAcceptableItem(ItemStack stack) {
                        return stack.isOf(Items.MACE);
                    }
                }
        );

        // Wind Charged ability
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.MACE)) return TypedActionResult.pass(stack);

            RegistryEntry<Enchantment> entry =
                    world.getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .getEntry(WIND_CHARGED_KEY)
                            .orElse(null);

            if (entry == null) return TypedActionResult.pass(stack);

            int level = EnchantmentHelper.getLevel(entry, stack);
            if (level <= 0) return TypedActionResult.pass(stack);

            if (player.getItemCooldownManager().isCoolingDown(stack.getItem()))
                return TypedActionResult.fail(stack);

            double launch = 1.0 + (level * 0.6);
            player.addVelocity(0, launch, 0);
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

            player.getItemCooldownManager().set(stack.getItem(), 100);
            return TypedActionResult.success(stack);
        });
    }
}
