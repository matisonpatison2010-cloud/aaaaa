package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.damage.DamageTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    // Enchantment registry keys
    public static final RegistryKey<Enchantment> WIND_CHARGED_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_charged"));

    public static final RegistryKey<Enchantment> ENDER_MIST_KEY =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "ender_mist"));

    // State tracking
    private static final Set<UUID> WIND_LAUNCHED = new HashSet<>();

    @Override
    public void onInitialize() {

        /* ================================
           RIGHT CLICK HANDLING
           ================================ */
        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (world.isClient) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty()) return TypedActionResult.pass(stack);

            /* -------- WIND CHARGED -------- */
            if (stack.getItem() == Items.MACE) {

                RegistryEntry<Enchantment> windEntry =
                        world.getRegistryManager()
                                .get(RegistryKeys.ENCHANTMENT)
                                .getEntry(WIND_CHARGED_KEY)
                                .orElse(null);

                if (windEntry != null) {
                    int level = EnchantmentHelper.getLevel(windEntry, stack);

                    if (level > 0 &&
                            !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {

                        double launchHeight = 0.42 + (level * 0.25); // tuned for ~5 blocks at lvl 1
                        player.addVelocity(0, launchHeight, 0);
                        player.velocityModified = true;
                        player.fallDistance = 0;

                        WIND_LAUNCHED.add(player.getUuid());

                        world.playSound(
                                null,
                                player.getBlockPos(),
                                SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST,
                                SoundCategory.PLAYERS,
                                1.0f,
                                1.0f
                        );

                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                    ParticleTypes.GUST,
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    30,
                                    0.3, 0.2, 0.3,
                                    0.02
                            );
                        }

                        player.getItemCooldownManager().set(stack.getItem(), 100); // 5s
                        return TypedActionResult.success(stack);
                    }
                }
            }

            /* -------- ENDER MIST -------- */
            RegistryEntry<Enchantment> mistEntry =
                    world.getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .getEntry(ENDER_MIST_KEY)
                            .orElse(null);

            if (mistEntry != null) {
                int level = EnchantmentHelper.getLevel(mistEntry, stack);

                if (level > 0 &&
                        !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {

                    int duration = (3 + level) * 20;

                    world.playSound(
                            null,
                            player.getBlockPos(),
                            SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                            SoundCategory.PLAYERS,
                            1.2f,
                            1.0f
                    );

                    if (world instanceof ServerWorld serverWorld) {
                        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
                                serverWorld,
                                player.getX(),
                                player.getY(),
                                player.getZ()
                        );

                        cloud.setOwner(player);
                        cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
                        cloud.setRadius(3.5f);
                        cloud.setDuration(duration);
                        cloud.setWaitTime(0);
                        cloud.setRadiusGrowth(-cloud.getRadius() / duration);

                        cloud.addEffect(new StatusEffectInstance(
                                StatusEffects.INSTANT_DAMAGE,
                                1,
                                1
                        ));

                        serverWorld.spawnEntity(cloud);
                    }

                    player.getItemCooldownManager().set(stack.getItem(), 20 * 20); // 20s
                    return TypedActionResult.success(stack);
                }
            }

            return TypedActionResult.pass(stack);
        });

        /* ================================
           DAMAGE CONTROL
           ================================ */

        // Prevent fall damage ONLY after Wind Charged
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {

                if (source.isOf(DamageTypes.FALL)
                        && WIND_LAUNCHED.contains(player.getUuid())) {
                    return false;
                }

                if (source.getSource() instanceof AreaEffectCloudEntity cloud
                        && cloud.getOwner() == player) {
                    return false;
                }
            }
            return true;
        });

        // Remove fall immunity on landing
        ServerLivingEntityEvents.AFTER_TICK.register(entity -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (player.isOnGround()) {
                    WIND_LAUNCHED.remove(player.getUuid());
                }
            }
        });
    }
}
