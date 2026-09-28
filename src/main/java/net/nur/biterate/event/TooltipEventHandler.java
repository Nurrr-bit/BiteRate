package net.nur.biterate.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.nur.biterate.BiteRate;
import net.nur.biterate.Config;
import net.nur.biterate.attachment.ModAttachments;
import net.nur.biterate.attachment.PlayerDietData;
import net.nur.biterate.util.EatingTimeCalculator;

@EventBusSubscriber(modid = BiteRate.MODID, value = Dist.CLIENT)
public class TooltipEventHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (Config.excludedFoods != null && Config.excludedFoods.contains(itemId.toString())) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
        long currentGameTime = player.level().getGameTime();
        int eatenCount = dietData.getEatenCount(itemId.toString(), currentGameTime);
        int maxDailyQuota = EatingTimeCalculator.calculateMaxDailyQuota(food);

        int currentFoodLevel = player.getFoodData().getFoodLevel();
        int originalDuration = stack.getUseDuration(player);

        int nutrition = food.nutrition();
        int excessNutrition = Math.max(0, nutrition - (BiteRate.MAX_FOOD_LEVEL - currentFoodLevel));

        int durationTicks;
        
        if (currentFoodLevel >= BiteRate.MAX_FOOD_LEVEL) {
            int excessEaten = Math.max(0, eatenCount - maxDailyQuota);
            double penaltyFactor = 1.0 + (excessEaten * Config.penaltyPerExcess);
            int penalizedDuration = (int) Math.round(originalDuration * penaltyFactor);
            durationTicks = Math.min(penalizedDuration, Config.maxDuration); // Ограничиваем капом
        } else {
            durationTicks = EatingTimeCalculator.calculateEatingDuration(food, originalDuration, currentFoodLevel, eatenCount, dietData.hasDietaryBonus());
        }
        double durationSeconds = EatingTimeCalculator.ticksToSeconds(durationTicks);

        int insertIndex = Math.min(1, event.getToolTip().size());

        Component timeInfo = Component.translatable(
                "tooltip.biterate.eat_time",
                String.format("%.1f", durationSeconds)
        ).withStyle(ChatFormatting.GRAY);
        event.getToolTip().add(insertIndex++, timeInfo);

        if (dietData.hasDietaryBonus() && excessNutrition > 0 && currentFoodLevel < BiteRate.MAX_FOOD_LEVEL) {
            int regenDuration = excessNutrition * 20;
            double regenSeconds = EatingTimeCalculator.ticksToSeconds(regenDuration);

            Component regenInfo = Component.translatable(
                    "tooltip.biterate.regen",
                    String.format("%.1f", regenSeconds)
            ).withStyle(ChatFormatting.GREEN);
            event.getToolTip().add(insertIndex++, regenInfo);
        } else {
            if (excessNutrition > 0 && currentFoodLevel < BiteRate.MAX_FOOD_LEVEL) {
                double slowdownSeconds = EatingTimeCalculator.ticksToSeconds(excessNutrition * Config.timeMultiplier);
                Component excessInfo = Component.translatable(
                        "tooltip.biterate.excess",
                        String.format("%.1f", slowdownSeconds)
                ).withStyle(ChatFormatting.GOLD);
                event.getToolTip().add(insertIndex++, excessInfo);
            }
        }

        
        MutableComponent limitInfo = Component.translatable(
                "tooltip.biterate.daily_limit",
                eatenCount,
                maxDailyQuota
        );
        if (eatenCount > maxDailyQuota) {
            limitInfo.withStyle(ChatFormatting.RED);
        } else if (eatenCount == maxDailyQuota) {
            limitInfo.withStyle(Style.EMPTY.withColor(0xFFA500)); // Красивый оранжевый цвет
        } else {
            limitInfo.withStyle(ChatFormatting.GREEN);
        }
        event.getToolTip().add(insertIndex++, limitInfo);

        if (eatenCount > maxDailyQuota) {
            int excessCount = eatenCount - maxDailyQuota;
            double maxPenaltyFactor = (double) Config.maxDuration / originalDuration;
            double maxExcessCount = (maxPenaltyFactor - 1.0) / Config.penaltyPerExcess;
            int cappedExcessCount = Math.min(excessCount, (int) Math.floor(maxExcessCount));

            int penaltyPercent = (int) Math.round(cappedExcessCount * Config.penaltyPerExcess * 100);
            Component penaltyInfo = Component.translatable(
                    "tooltip.biterate.penalty",
                    penaltyPercent
            ).withStyle(ChatFormatting.RED);
            event.getToolTip().add(insertIndex++, penaltyInfo);
        }

        
        String bonusStatusKey = dietData.hasDietaryBonus() ? "tooltip.biterate.status.yes" : "tooltip.biterate.status.no";

        MutableComponent bonusInfo = Component.translatable(
                "tooltip.biterate.dietary_bonus",
                Component.translatable(bonusStatusKey)
        );

        if (dietData.hasDietaryBonus()) {
            if (dietData.hadPenaltiesToday() || eatenCount > maxDailyQuota) {
                
                bonusInfo.withStyle(Style.EMPTY.withColor(0xFFA500));
            } else {
                
                bonusInfo.withStyle(ChatFormatting.GREEN);
            }
        } else {
            bonusInfo.withStyle(ChatFormatting.GRAY);
        }
        event.getToolTip().add(insertIndex, bonusInfo);
    }
}
