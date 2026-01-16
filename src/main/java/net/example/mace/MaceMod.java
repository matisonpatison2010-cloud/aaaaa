
package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class MaceMod implements ModInitializer {

    public static final Enchantment WIND_CHARGED =
        Registry.register(
            Registries.ENCHANTMENT,
            new Identifier("mace", "wind_charged"),
            new WindChargedEnchantment()
        );

    private static final HashMap<UUID, Integer> cooldowns = new HashMap<>();

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (!(stack.getItem() instanceof MaceItem)) {
                return TypedActionResult.pass(stack);
            }

            int level = EnchantmentHelper.getLevel(WIND_CHARGED, stack);
            if (level <= 0) return TypedActionResult.pass(stack);

            if (cooldowns.getOrDefault(player.getUuid(), 0) > 0) {
                return TypedActionResult.fail(stack);
            }

            double power = 0.9 + (level * 0.6);
            player.addVelocity(0, power, 0);
            player.velocityModified = true;

            world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_WIND_CHARGE_THROW,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
            );

            for (int i = 0; i < 30; i++) {
                world.addParticle(
                    ParticleTypes.GUST,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    0, 0.1, 0
                );
            }

            player.fallDistance = 0;
            cooldowns.put(player.getUuid(), 100);

            return TypedActionResult.success(stack);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            cooldowns.replaceAll((id, time) -> time - 1);
            cooldowns.entrySet().removeIf(e -> e.getValue() <= 0);
        });
    }

    private static class WindChargedEnchantment extends Enchantment {
        protected WindChargedEnchantment() {
            super(Rarity.RARE, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        }

        @Override
        public boolean isAcceptableItem(ItemStack stack) {
            return stack.getItem() instanceof MaceItem;
        }

        @Override
        public int getMaxLevel() {
            return 3;
        }
    }
}
