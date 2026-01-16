
package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    public static final RegistryKey<Enchantment> WIND_CHARGED_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_charged"));

    private static final Set<UUID> NO_FALL_DAMAGE = new HashSet<>();

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() != Items.MACE) return TypedActionResult.pass(stack);

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

            double launchVelocity = 1.0 + (level * 0.6);
            player.addVelocity(0, launchVelocity, 0);
            player.velocityModified = true;

            NO_FALL_DAMAGE.add(player.getUuid());

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(),
                        SoundCategory.PLAYERS,
                        1.2f,
                        1.0f
                );

                serverWorld.spawnParticles(
                        ParticleTypes.GUST,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        25,
                        0.5, 0.2, 0.5,
                        0.12
                );
            }

            player.getItemCooldownManager().set(stack.getItem(), 100);
            return TypedActionResult.success(stack);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            DamageSource fall = player.getWorld().getDamageSources().fall();
            if (source == fall && NO_FALL_DAMAGE.remove(player.getUuid())) {
                return false;
            }
            return true;
        });
    }
}
