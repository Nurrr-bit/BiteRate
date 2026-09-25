package net.nur.biterate.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.nur.biterate.BiteRate;
import net.nur.biterate.attachment.ModAttachments;
import net.nur.biterate.attachment.PlayerDietData;

import java.util.HashMap;
import java.util.Map;

public record SyncDietDataPayload(long gameTime, Map<String, Integer> eatenMap, boolean hasDietaryBonus, long bonusActivatedGameTime, boolean hadPenaltiesToday) implements CustomPacketPayload {

    public static final Type<SyncDietDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BiteRate.MODID, "sync_diet_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDietDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SyncDietDataPayload::gameTime,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            SyncDietDataPayload::eatenMap,
            ByteBufCodecs.BOOL,
            SyncDietDataPayload::hasDietaryBonus,
            ByteBufCodecs.VAR_LONG,
            SyncDietDataPayload::bonusActivatedGameTime,
            ByteBufCodecs.BOOL,
            SyncDietDataPayload::hadPenaltiesToday,
            SyncDietDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SyncDietDataPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                PlayerDietData dietData = player.getData(ModAttachments.DIET_DATA);
                dietData.setMapDirectly(payload.gameTime(), payload.eatenMap(), payload.hasDietaryBonus(), payload.bonusActivatedGameTime(), payload.hadPenaltiesToday());
            }
        });
    }
}