package net.nur.biterate.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.nur.biterate.BiteRate;
import net.nur.biterate.Config;
import net.nur.biterate.attachment.ModAttachments;
import net.nur.biterate.attachment.PlayerDietData;
import net.nur.biterate.util.EatingTimeCalculator;

@EventBusSubscriber(modid = BiteRate.MODID, value = Dist.CLIENT)
public class EatingParticleHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = event.getEntity();

            
            if (player != mc.player) return;

            
            if (!player.isUsingItem()) return;

            ItemStack stack = player.getUseItem();
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food == null) return;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Config.excludedFoods != null && Config.excludedFoods.contains(itemId.toString())) return;

            
            PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
            long currentGameTime = player.level().getGameTime();
            int eatenCount = dietData.getEatenCount(itemId.toString(), currentGameTime);

            int currentFoodLevel = player.getFoodData().getFoodLevel();
            int originalDuration = stack.getUseDuration(player);
            int totalDuration = EatingTimeCalculator.calculateEatingDuration(food, originalDuration, currentFoodLevel, eatenCount);

            
            if (totalDuration == originalDuration) return;

            int remainingTicks = player.getUseItemRemainingTicks();
            int elapsed = totalDuration - remainingTicks;

            
            int vanillaInterval = 4;
            int vanillaDuration = 32;
            int adaptedInterval = (totalDuration * vanillaInterval) / vanillaDuration;

            if (elapsed % adaptedInterval == 0 && elapsed > 0) {
                
                int particleCount = 3 + player.getRandom().nextInt(3);
                spawnEatingParticles(player, stack, particleCount);
            }
        }
    }

    private static void spawnEatingParticles(Player player, ItemStack stack, int count) {
        double x = player.getX();
        double y = player.getEyeY() - 0.2;
        double z = player.getZ();

        for (int i = 0; i < count; i++) {
            double offsetX = (player.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = (player.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetZ = (player.getRandom().nextDouble() - 0.5) * 0.5;

            
            double velocityX = (player.getRandom().nextDouble() - 0.5) * 0.1;
            double velocityY = (player.getRandom().nextDouble() - 0.5) * 0.1;
            double velocityZ = (player.getRandom().nextDouble() - 0.5) * 0.1;

            player.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack),
                    x + offsetX, y + offsetY, z + offsetZ, velocityX, velocityY, velocityZ);
        }
    }
}
