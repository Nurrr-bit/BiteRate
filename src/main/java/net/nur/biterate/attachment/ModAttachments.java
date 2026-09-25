package net.nur.biterate.attachment;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.nur.biterate.BiteRate;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BiteRate.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerDietData>> DIET_DATA =
            ATTACHMENT_TYPES.register("diet_data", () ->
                    AttachmentType.serializable(PlayerDietData::new)
                            .build()
            );
}