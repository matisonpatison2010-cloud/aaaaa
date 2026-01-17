package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MaceMod implements ModInitializer {

    public static final String MODID = "mace";

    // Enchantment registry keys
    public static final RegistryKey<Enchantment> WIND_CHARGED =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, new Identifier(MODID, "wind_charged"));

    public static final RegistryKey<Enchantment> ENDER_MIST =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, new Identifier(MODID, "ender_mist"));

    // Tracks players launched by Wind Charged
    private static final Set<UUID> WIND_LAUNCHED = new HashSet<>();

    @Override
    public void onInitialize() {

        // RIGHT CLICK HANDLER
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);

            RegistryEntry<Enchantment> wind = getEnchant(world, WIND_CHARGED);
            RegistryEntry<Enchantment> mist = getEnchant(world, ENDER_MIST);

            int windLevel = wind != null ? EnchantmentHelper.getLevel(wind, stack) : 0;
            int mistLevel = mist != null ? EnchantmentHelper.getLevel(mist, stack) : 0;

            // Incompatible enchantments
            if (windLevel > 0 && mistLevel > 0) {
                return TypedActionResult.fail(stack);
            }

            /* ---------------- WIND CHARGED ---------------- */
            if (stack.isOf(Items.MACE) && windLevel > 0) {

                if (player.getItemCooldownManager().isCoolingDown(stack.getItem()))
                    return TypedActionResult.fail(stack);

                double velocity = 1.0 + (windLevel * 0.6); // ~5 blocks at level 1
                player.addVelocity(0, velocity, 0);
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

                world.spawnParticles(
                        ParticleTypes.GUST,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        20,
                        0.3,
                        0.1,
                        0.3,
                        0.05
                );

                player.getItemCooldownManager().set(stack.getItem(), 100); // 5s
                return TypedActionResult.success(stack);
            }

            /* ---------------- ENDER MIST ---------------- */
            if (mistLevel > 0 &&
                    (
                            stack.isIn(ItemTags.SWORDS)
                                    || stack.isIn(ItemTags.AXES)
                                    || stack.isIn(ItemTags.PICKAXES)
                                    || stack.isIn(ItemTags.SHOVELS)
                                    || stack.isIn(ItemTags.HOES)
                                    || stack.isOf(Items.MACE)
                    )) {

                if (player.getItemCooldownManager().isCoolingDown(stack.getItem()))
                    return TypedActionResult.fail(stack);

                AreaEffectCloudEntity cloud =
                        new AreaEffectCloudEntity(world, player.getX(), player.getY(), player.getZ());

                cloud.setRadius(3.0F);
                cloud.setDuration((3 + mistLevel) * 20); // seconds
                cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
                cloud.setOwner(player);

                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.INSTANT_DAMAGE,
                        1,
                        mistLevel - 1
                ));

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

            return TypedActionResult.pass(stack);
        });

        /* -------- FIX FALL DAMAGE BUG -------- */
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.isOnGround()) {
                    WIND_LAUNCHED.remove(player.getUuid());
                }
            }
        });
    }

    private static RegistryEntry<Enchantment> getEnchant(World world, RegistryKey<Enchantment> key) {
        return world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(key)
                .orElse(null);
    }
}
