package net.nur.biterate;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue TIME_MULTIPLIER = BUILDER
            .comment("Множитель времени для избытка сытости")
            .defineInRange("timeMultiplier", 4, 1, 20);

    private static final ModConfigSpec.IntValue MAX_DURATION = BUILDER
            .comment("Максимальное время поедания в тиках (Cap)")
            .defineInRange("maxDuration", 100, 1, 200);

    private static final ModConfigSpec.IntValue DAILY_NUTRITION_CAP = BUILDER
            .comment("Базовый суточный лимит сытости для одной еды (Суточный лимит штук = Лимит сытости / Сытость еды)")
            .defineInRange("dailyNutritionCap", 32, 1, 100);

    private static final ModConfigSpec.DoubleValue PENALTY_PER_EXCESS = BUILDER
            .comment("Множитель штрафа к времени за каждую порцию сверх дневной нормы (0.5 = +50% ко времени за каждую порцию)")
            .defineInRange("penaltyPerExcess", 0.5, 0.0, 5.0);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_FOODS = BUILDER
            .comment("Список предметов, исключенных из изменения времени поедания")
            .defineListAllowEmpty("excludedFoods", List.of(), () -> "", o -> o instanceof String); // <-- Убрали торт

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int timeMultiplier = 4;
    public static int maxDuration = 100;
    public static int dailyNutritionCap = 32;
    public static double penaltyPerExcess = 0.5;
    public static Set<String> excludedFoods = Set.of(); // <-- Пустой сет по умолчанию

    static void onLoad(final ModConfigEvent event) {
        timeMultiplier = TIME_MULTIPLIER.get();
        maxDuration = MAX_DURATION.get();
        dailyNutritionCap = DAILY_NUTRITION_CAP.get();
        penaltyPerExcess = PENALTY_PER_EXCESS.get();
        excludedFoods = EXCLUDED_FOODS.get() != null ? Set.copyOf(EXCLUDED_FOODS.get()) : Set.of(); // <-- Возвращаем пустой сет, если что-то не так
    }
}