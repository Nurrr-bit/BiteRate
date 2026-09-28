package net.nur.biterate;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.nur.biterate.attachment.ModAttachments;
import net.nur.biterate.network.SyncDietDataPayload;
import org.slf4j.Logger;

@Mod(BiteRate.MODID)
public class BiteRate {
    public static final String MODID = "biterate";
    public static final Logger LOGGER = LogUtils.getLogger();

    
    public static final int TICKS_PER_SECOND = 20;
    public static final long TICKS_PER_DAY = 24000L; // <-- Добавили константу суток
    public static final int MAX_FOOD_LEVEL = 20;

    public BiteRate(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(Config::onLoad);

        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            event.registrar("1").playToClient(
                    SyncDietDataPayload.TYPE,
                    SyncDietDataPayload.STREAM_CODEC,
                    SyncDietDataPayload::handle
            );
        });
    }
}
