package net.example.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MaceMod implements ModInitializer {

    public static final String MODID = "macemod";

    @Override
    public void onInitialize() {

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != player.getActiveHand()) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            // MAIN HAND ONLY
            if (hand != net.minecraft.util.Hand.MAIN_HAND) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getStackInHand(hand);

            // MUST BE A MACE
            if (!stack.isOf(Items.MACE)) {
                return TypedActionResult.pass(stack);
            }

            // READ CUSTOM DATA (1.21+ SAFE)
            NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (component == null) {
                return TypedActionResult.pass(stack);
            }

            NbtCompound root = component.copyNbt();
            if (!root.contains(MODID)) {
                return TypedActionResult.pass(stack);
            }

            NbtCompound macemod = root.getCompound(MODID);

            int windCharged = macemod.getInt("wind_charged");
            int enderMist = macemod.getInt("ender_mist");

            if (windCharged <= 0 && enderMist <= 0) {
                return TypedActionResult.pass(stack);
            }

            // COOLDOWN
            player.getItemCooldownManager().set(Items.MACE, 40);

            if (!world.isClient) {

                Vec3d look = player.getRotationVec(1.0F);
                player.addVelocity(
                        look.x * (0.6 + 0.2 * windCharged),
                        0.5 + 0.15 * windCharged,
                        look.z * (0.6 + 0.2 * windCharged)
                );
                player.velocityModified = true;

                world.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_WIND_CHARGE_THROW,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );

                if (enderMist > 0) {
                    player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.INVISIBILITY,
                            40 + enderMist * 20,
                            0,
                            false,
                            false
                    ));
                }
            }

            if (world.isClient) {
                for (int i = 0; i < 20; i++) {
                    world.addParticle(
                            ParticleTypes.CLOUD,
                            player.getX(),
                            player.getY() + 1.0,
                            player.getZ(),
                            0.0,
                            0.1,
                            0.0
                    );
                }
            }

            return TypedActionResult.success(stack);
        });
    }
}
