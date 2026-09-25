package net.nur.biterate.util;

import net.minecraft.world.food.FoodProperties;
import net.nur.biterate.BiteRate;
import net.nur.biterate.Config;

/**
 * Утилитный класс для вычисления времени поедания еды.
 */
public final class EatingTimeCalculator {

    private EatingTimeCalculator() {}

    /**
     * Вычисляет максимальную дневную норму штук для указанной еды.
     */
    public static int calculateMaxDailyQuota(FoodProperties food) {
        int nutrition = Math.max(1, food.nutrition());
        return Math.max(1, Config.dailyNutritionCap / nutrition);
    }

    /**
     * Вычисляет время поедания в тиках с учетом уровня голода и съеденных сегодня порций.
     *
     * @param food Свойства еды
     * @param originalDuration Оригинальное время поедания из ванильного Minecraft
     * @param currentFoodLevel Текущий уровень голода игрока
     * @param eatenCountToday Количество штук этой еды, съеденных за текущий день
     * @param hasDietaryBonus Активен ли бонус (избыток не увеличивает время)
     * @return Время поедания в тиках
     */
    public static int calculateEatingDuration(FoodProperties food, int originalDuration, int currentFoodLevel, int eatenCountToday, boolean hasDietaryBonus) {
        int nutrition = food.nutrition();

        // Вычисляем избыток сытости
        int excessNutrition = Math.max(0, nutrition - (BiteRate.MAX_FOOD_LEVEL - currentFoodLevel));

        // Если бонус активен, избыток не увеличивает время
        int baseDuration;
        if (hasDietaryBonus) {
            baseDuration = originalDuration;
        } else {
            baseDuration = originalDuration + (excessNutrition * Config.timeMultiplier);
        }

        // Если базовое время уже достигло cap, не применяем штраф
        if (baseDuration >= Config.maxDuration) {
            return Config.maxDuration;
        }

        // Расчет дневного штрафа за превышение лимита
        int maxDailyQuota = calculateMaxDailyQuota(food);
        int excessEaten = Math.max(0, eatenCountToday - maxDailyQuota);

        double penaltyFactor = 1.0 + (excessEaten * Config.penaltyPerExcess);
        int finalDuration = (int) Math.round(baseDuration * penaltyFactor);

        // Cap - ограничиваем максимальное время
        return Math.min(finalDuration, Config.maxDuration);
    }

    /**
     * Вычисляет время поедания в тиках с учетом уровня голода и съеденных сегодня порций (без бонуса).
     *
     * @param food Свойства еды
     * @param originalDuration Оригинальное время поедания из ванильного Minecraft
     * @param currentFoodLevel Текущий уровень голода игрока.
     * @param eatenCountToday Количество штук этой еды, съеденных за текущий день
     * @return Время поедания в тиках
     */
    public static int calculateEatingDuration(FoodProperties food, int originalDuration, int currentFoodLevel, int eatenCountToday) {
        return calculateEatingDuration(food, originalDuration, currentFoodLevel, eatenCountToday, false);
    }

    /**
     * Конвертирует тики в секунды.
     *
     * @param ticks Время в тиках
     * @return Время в секундах
     */
    public static double ticksToSeconds(int ticks) {
        return ticks / (double) BiteRate.TICKS_PER_SECOND;
    }
}
