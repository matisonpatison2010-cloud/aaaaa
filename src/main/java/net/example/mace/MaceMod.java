package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.LivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    // Enchantment registry keys (DATA-DRIVEN)
    public static final RegistryKey<Enchantment> WIND_CHARGED_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_charged"));

    public static final RegistryKey<Enchantment> ENDER_MIST_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "ender_mist"));

    @Override
    public void onInitialize() {

        /* ===============================
           RIGHT CLICK ENCHANT LOGIC
           =============================== */
        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);

            RegistryEntry<Enchantment> windEntry =
                    world.getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .getEntry(WIND_CHARGED_KEY)
                            .orElse(null);

            RegistryEntry<Enchantment> mistEntry =
                    world.getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .getEntry(ENDER_MIST_KEY)
                            .orElse(null);

            /* ========= WIND CHARGED ========= */
            if (stack.isOf(Items.MACE) && windEntry != null) {

                int level = EnchantmentHelper.getLevel(windEntry, stack);
                if (level > 0 && player instanceof ServerPlayerEntity serverPlayer) {

                    if (serverPlayer.getItemCooldownManager().isCoolingDown(stack.getItem()))
                        return TypedActionResult.fail(stack);

                    double launch = 1.2 + (level * 0.6);
                    serverPlayer.addVelocity(0, launch, 0);
                    serverPlayer.velocityModified = true;

                    // mark player so ONLY THIS jump cancels fall damage
                    serverPlayer.getPersistentData().putBoolean("wind_charged_launch", true);

                    world.playSound(
                            null,
                            serverPlayer.getBlockPos(),
                            SoundEvents.ENTITY_WIND_CHARGE_THROW,
                            SoundCategory.PLAYERS,
                            1f,
                            1f
                    );

                    serverPlayer.getItemCooldownManager().set(stack.getItem(), 100); // 5s
                    return TypedActionResult.success(stack);
                }
            }

            /* ========= ENDER MIST ========= */
            if (mistEntry != null) {

                int level = EnchantmentHelper.getLevel(mistEntry, stack);
                if (level > 0 && player instanceof ServerPlayerEntity serverPlayer) {

                    if (serverPlayer.getItemCooldownManager().isCoolingDown(stack.getItem()))
                        return TypedActionResult.fail(stack);

                    int duration = 60 + (level * 20); // 3s + 1s per level

                    AreaEffectCloudEntity cloud =
                            new AreaEffectCloudEntity(world,
                                    serverPlayer.getX(),
                                    serverPlayer.getY(),
                                    serverPlayer.getZ());

                    cloud.setRadius(3.5F);
                    cloud.setDuration(duration);
                    cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
                    cloud.setOwner(serverPlayer);

                    world.spawnEntity(cloud);

                    world.playSound(
                            null,
                            serverPlayer.getBlockPos(),
                            SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                            SoundCategory.PLAYERS,
                            1f,
                            1f
                    );

                    serverPlayer.getItemCooldownManager().set(stack.getItem(), 400); // 20s
                    return TypedActionResult.success(stack);
                }
            }

            return TypedActionResult.pass(stack);
        });

        /* ===============================
           DAMAGE CONTROL
           =============================== */
        LivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (entity instanceof ServerPlayerEntity player) {

                // Wind Charged: cancel ONLY ONE fall
                if (source.isOf(DamageTypes.FALL)
                        && player.getPersistentData().getBoolean("wind_charged_launch")) {

                    player.getPersistentData().remove("wind_charged_launch");
                    return false;
                }

                // Ender Mist: immune to own dragon breath
                if (source.isOf(DamageTypes.DRAGON_BREATH)
                        && source.getAttacker() == entity) {
                    return false;
                }
            }

            return true;
        });
    }
}
