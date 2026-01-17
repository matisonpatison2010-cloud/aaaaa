package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "macemod";

    public static Enchantment WIND_CHARGED;
    public static Enchantment ENDER_MIST;

    @Override
    public void onInitialize() {

        WIND_CHARGED = Registry.register(
                Registries.ENCHANTMENT,
                Identifier.of(MODID, "wind_charged"),
                new SimpleMaceEnchantment(Enchantment.Rarity.RARE)
        );

        ENDER_MIST = Registry.register(
                Registries.ENCHANTMENT,
                Identifier.of(MODID, "ender_mist"),
                new SimpleMaceEnchantment(Enchantment.Rarity.VERY_RARE) {
                    @Override
                    public boolean isTreasure() {
                        return true; // makes it unobtainable normally
                    }

                    @Override
                    public boolean isAvailableForEnchantedBookOffer() {
                        return false;
                    }

                    @Override
                    public boolean isAvailableForRandomSelection() {
                        return false;
                    }
                }
        );
    }

    /**
     * Call this from wherever your mace attack logic runs
     */
    public static void onMaceHit(World world, LivingEntity target, PlayerEntity attacker, ItemStack stack) {

        int wind = EnchantmentHelper.getLevel(WIND_CHARGED, stack);
        int mist = EnchantmentHelper.getLevel(ENDER_MIST, stack);

        if (!world.isClient) {

            // WIND CHARGED FIX
            if (wind > 0) {
                Vec3d push = attacker.getRotationVector().multiply(1.2 + wind * 0.6);
                target.addVelocity(push.x, 0.4 + wind * 0.2, push.z);
                target.velocityModified = true;
            }

            // ENDER MIST EFFECT
            if (mist > 0) {
                Box box = target.getBoundingBox().expand(3.5);

                for (LivingEntity e : world.getEntitiesByClass(
                        LivingEntity.class,
                        box,
                        entity -> entity != attacker
                )) {
                    e.addStatusEffect(
                            new StatusEffectInstance(
                                    StatusEffects.WITHER,
                                    60,
                                    mist - 1
                            )
                    );
                }

                // particles
                world.spawnParticles(
                        ParticleTypes.DRAGON_BREATH,
                        target.getX(),
                        target.getY() + 1,
                        target.getZ(),
                        40,
                        0.6,
                        0.4,
                        0.6,
                        0.02
                );
            }
        }
    }

    /**
     * Dragon breath sound (when firing, NOT impact)
     */
    public static void playDragonBreathSound(World world, EnderDragonEntity dragon) {
        world.playSound(
                null,
                dragon.getBlockPos(),
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                SoundCategory.HOSTILE,
                2.5f,
                1.0f
        );
    }
}
