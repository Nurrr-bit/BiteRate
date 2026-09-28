package net.nur.biterate.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.nur.biterate.BiteRate; // <-- Импортируем главный класс для доступа к константе

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerDietData implements INBTSerializable<CompoundTag> {

    private long lastResetGameTime = 0;
    private final Map<String, Integer> eatenFoodsMap = new HashMap<>();
    private boolean hasDietaryBonus = false;
    private long bonusActivatedGameTime = 0;
    private boolean hadPenaltiesToday = false;

    public Map<String, Integer> getEatenFoodsMap() {
        return Collections.unmodifiableMap(eatenFoodsMap);
    }

    public void resetDailyDiet(long currentGameTime) {
        lastResetGameTime = currentGameTime;
        eatenFoodsMap.clear();
    }

    private void checkAndResetByTime(long currentGameTime) {
        if (lastResetGameTime == 0) {
            // При первой инициализации выравниваем по началу текущего дня
            lastResetGameTime = (currentGameTime / BiteRate.TICKS_PER_DAY) * BiteRate.TICKS_PER_DAY;
            return;
        }
        long ticksSinceReset = currentGameTime - lastResetGameTime;
        if (ticksSinceReset >= BiteRate.TICKS_PER_DAY) {
            resetDailyDiet(currentGameTime);
            hadPenaltiesToday = false; // Сбрасываем флаг штрафов через день
        }

        
        if (hasDietaryBonus && bonusActivatedGameTime > 0) {
            long ticksSinceBonusActivation = currentGameTime - bonusActivatedGameTime;
            if (ticksSinceBonusActivation >= BiteRate.TICKS_PER_DAY) {
                hasDietaryBonus = false;
                bonusActivatedGameTime = 0;
            }
        }
    }

    public int getEatenCount(String itemId, long currentGameTime) {
        checkAndResetByTime(currentGameTime);
        return eatenFoodsMap.getOrDefault(itemId, 0);
    }

    public int incrementEatenCount(String itemId, long currentGameTime) {
        checkAndResetByTime(currentGameTime);
        int newCount = eatenFoodsMap.getOrDefault(itemId, 0) + 1;
        eatenFoodsMap.put(itemId, newCount);
        return newCount;
    }

    public void setMapDirectly(long gameTime, Map<String, Integer> map) {
        this.lastResetGameTime = gameTime;
        this.eatenFoodsMap.clear();
        this.eatenFoodsMap.putAll(map);
    }

    public void setMapDirectly(long gameTime, Map<String, Integer> map, boolean hasDietaryBonus) {
        this.lastResetGameTime = gameTime;
        this.eatenFoodsMap.clear();
        this.eatenFoodsMap.putAll(map);
        this.hasDietaryBonus = hasDietaryBonus;
    }

    public void setMapDirectly(long gameTime, Map<String, Integer> map, boolean hasDietaryBonus, long bonusActivatedGameTime) {
        this.lastResetGameTime = gameTime;
        this.eatenFoodsMap.clear();
        this.eatenFoodsMap.putAll(map);
        this.hasDietaryBonus = hasDietaryBonus;
        this.bonusActivatedGameTime = bonusActivatedGameTime;
    }

    public void setMapDirectly(long gameTime, Map<String, Integer> map, boolean hasDietaryBonus, long bonusActivatedGameTime, boolean hadPenaltiesToday) {
        this.lastResetGameTime = gameTime;
        this.eatenFoodsMap.clear();
        this.eatenFoodsMap.putAll(map);
        this.hasDietaryBonus = hasDietaryBonus;
        this.bonusActivatedGameTime = bonusActivatedGameTime;
        this.hadPenaltiesToday = hadPenaltiesToday;
    }

    public boolean hasDietaryBonus() {
        return hasDietaryBonus;
    }

    public long getBonusActivatedGameTime() {
        return bonusActivatedGameTime;
    }

    public boolean hadPenaltiesToday() {
        return hadPenaltiesToday;
    }

    public void setHadPenaltiesToday(boolean hadPenalties) {
        this.hadPenaltiesToday = hadPenalties;
    }

    public void setDietaryBonus(boolean hasBonus) {
        this.hasDietaryBonus = hasBonus;
    }

    public void setDietaryBonus(boolean hasBonus, long gameTime) {
        this.hasDietaryBonus = hasBonus;
        if (hasBonus) {
            this.bonusActivatedGameTime = gameTime;
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("lastResetGameTime", lastResetGameTime);
        tag.putBoolean("hasDietaryBonus", hasDietaryBonus);
        tag.putLong("bonusActivatedGameTime", bonusActivatedGameTime);
        tag.putBoolean("hadPenaltiesToday", hadPenaltiesToday);

        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : eatenFoodsMap.entrySet()) {
            mapTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("eatenFoods", mapTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        lastResetGameTime = tag.getLong("lastResetGameTime");
        hasDietaryBonus = tag.getBoolean("hasDietaryBonus");
        bonusActivatedGameTime = tag.getLong("bonusActivatedGameTime");
        hadPenaltiesToday = tag.getBoolean("hadPenaltiesToday");
        eatenFoodsMap.clear();

        if (tag.contains("eatenFoods")) {
            CompoundTag mapTag = tag.getCompound("eatenFoods");
            for (String key : mapTag.getAllKeys()) {
                eatenFoodsMap.put(key, mapTag.getInt(key));
            }
        }
    }
}
