package net.nur.biterate.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nur.biterate.BiteRate;
import net.nur.biterate.Config;
import net.nur.biterate.attachment.ModAttachments;
import net.nur.biterate.attachment.PlayerDietData;
import net.nur.biterate.network.SyncDietDataPayload;
import net.nur.biterate.util.EatingTimeCalculator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = BiteRate.MODID)
public class EatingDurationHandler {

    // Хранилище, чтобы запоминать реальный уровень голода ДО того, как еда восстановит сытость
    private static final Map<UUID, Integer> PRE_EAT_FOOD_LEVELS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack stack = event.getItem();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (Config.excludedFoods != null && Config.excludedFoods.contains(itemId.toString())) return;

        int currentFoodLevel = player.getFoodData().getFoodLevel();
        // Запоминаем уровень голода в момент начала еды
        PRE_EAT_FOOD_LEVELS.put(player.getUUID(), currentFoodLevel);

        PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
        long currentGameTime = player.level().getGameTime();
        int eatenCount = dietData.getEatenCount(itemId.toString(), currentGameTime);

        int originalDuration = event.getDuration();
        int newDuration;

        // Если игрок полностью сыт, применяем ТОЛЬКО штрафы (без избытка)
        if (currentFoodLevel >= BiteRate.MAX_FOOD_LEVEL) {
            int maxDailyQuota = EatingTimeCalculator.calculateMaxDailyQuota(food);
            int excessEaten = Math.max(0, eatenCount - maxDailyQuota);
            double penaltyFactor = 1.0 + (excessEaten * Config.penaltyPerExcess);
            int penalizedDuration = (int) Math.round(originalDuration * penaltyFactor);
            newDuration = Math.min(penalizedDuration, Config.maxDuration);
        } else {
            newDuration = EatingTimeCalculator.calculateEatingDuration(food, originalDuration, currentFoodLevel, eatenCount, dietData.hasDietaryBonus());
        }

        event.setDuration(newDuration);
    }

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Обновляем показатель каждый тик, чтобы получить самый точный уровень голода перед концом еды
        PRE_EAT_FOOD_LEVELS.put(player.getUUID(), player.getFoodData().getFoodLevel());
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Если игрок передумал и перестал есть - очищаем память
        PRE_EAT_FOOD_LEVELS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // ДОСТАЕМ уровень голода, который был ПЕРЕД тем как еда восстановила сытость
        int preEatFoodLevel = PRE_EAT_FOOD_LEVELS.getOrDefault(player.getUUID(), player.getFoodData().getFoodLevel());
        PRE_EAT_FOOD_LEVELS.remove(player.getUUID());

        ItemStack stack = event.getItem();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (Config.excludedFoods != null && Config.excludedFoods.contains(itemId.toString())) return;

        PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
        long currentGameTime = player.level().getGameTime();
        int eatenCount = dietData.incrementEatenCount(itemId.toString(), currentGameTime);

        int maxDailyQuota = EatingTimeCalculator.calculateMaxDailyQuota(food);
        if (eatenCount > maxDailyQuota) {
            dietData.setHadPenaltiesToday(true);
        }

        if (dietData.hasDietaryBonus()) {
            int nutrition = food.nutrition();

            // Считаем избыток на основе голода ДО поедания!
            int excessNutrition = Math.max(0, nutrition - (BiteRate.MAX_FOOD_LEVEL - preEatFoodLevel));

            // Реген дается только если был РЕАЛЬНЫЙ избыток и игрок не был уже полностью сыт до еды
            if (excessNutrition > 0 && preEatFoodLevel < BiteRate.MAX_FOOD_LEVEL) {
                int regenDuration = excessNutrition * 20;
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, 0, false, false));
            }
        }

        PacketDistributor.sendToPlayer(player, new SyncDietDataPayload(currentGameTime, dietData.getEatenFoodsMap(), dietData.hasDietaryBonus(), dietData.getBonusActivatedGameTime(), dietData.hadPenaltiesToday()));
    }

    // ИСПРАВЛЕНИЕ 2: Копируем бонусные данные при возрождении/смерти
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        PlayerDietData oldData = original.getData(ModAttachments.DIET_DATA);
        PlayerDietData newData = newPlayer.getData(ModAttachments.DIET_DATA);

        if (event.isWasDeath()) {
            // Если игрок умер: переносим только бонус и флаг штрафа (счетчики съеденной еды сбрасываются)
            newData.setDietaryBonus(oldData.hasDietaryBonus(), oldData.getBonusActivatedGameTime());
            newData.setHadPenaltiesToday(oldData.hadPenaltiesToday());
        } else {
            // Если игрок возвращается из Энда (прошел через портал) - копируем абсолютно всё
            newData.deserializeNBT(newPlayer.registryAccess(), oldData.serializeNBT(original.registryAccess()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncDietData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
            long currentGameTime = player.level().getGameTime();

            // Сбрасываем счетчик еды, но теперь бонус останется благодаря onPlayerClone!
            dietData.resetDailyDiet(currentGameTime);
            syncDietData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncDietData(player);
        }
    }

    private static void syncDietData(ServerPlayer player) {
        PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
        long currentGameTime = player.level().getGameTime();
        PacketDistributor.sendToPlayer(player, new SyncDietDataPayload(currentGameTime, dietData.getEatenFoodsMap(), dietData.hasDietaryBonus(), dietData.getBonusActivatedGameTime(), dietData.hadPenaltiesToday()));
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        ServerLevel level = (ServerLevel) event.getLevel();
        long currentGameTime = level.getGameTime();

        for (ServerPlayer player : level.players()) {
            PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);

            dietData.setDietaryBonus(!dietData.hadPenaltiesToday(), currentGameTime);
            dietData.setHadPenaltiesToday(false);
            dietData.resetDailyDiet(currentGameTime);

            PacketDistributor.sendToPlayer(player, new SyncDietDataPayload(currentGameTime, dietData.getEatenFoodsMap(), dietData.hasDietaryBonus(), dietData.getBonusActivatedGameTime(), dietData.hadPenaltiesToday()));
        }
    }
}