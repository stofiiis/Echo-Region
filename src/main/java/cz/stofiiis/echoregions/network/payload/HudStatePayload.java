package cz.stofiiis.echoregions.network.payload;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HudStatePayload(
        boolean enabled,
        int regionX,
        int regionZ,
        String dimensionNamespace,
        String dimensionPath,
        String headlineId,
        int headlineIntensity,
        List<TagSummary> tags,
        int totalWeight
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("echoregions", "hud_state");
    public static final Type<HudStatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TagSummary> TAG_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TagSummary::stateId,
            ByteBufCodecs.VAR_INT, TagSummary::intensity,
            TagSummary::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HudStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HudStatePayload::enabled,
            ByteBufCodecs.VAR_INT, HudStatePayload::regionX,
            ByteBufCodecs.VAR_INT, HudStatePayload::regionZ,
            ByteBufCodecs.STRING_UTF8, HudStatePayload::dimensionNamespace,
            ByteBufCodecs.STRING_UTF8, HudStatePayload::dimensionPath,
            ByteBufCodecs.STRING_UTF8, HudStatePayload::headlineId,
            ByteBufCodecs.VAR_INT, HudStatePayload::headlineIntensity,
            TAG_CODEC.apply(ByteBufCodecs.list()), HudStatePayload::tags,
            ByteBufCodecs.VAR_INT, HudStatePayload::totalWeight,
            HudStatePayload::new
    );

    public static HudStatePayload disabled() {
        return new HudStatePayload(false, 0, 0, "minecraft", "overworld", "neutral", 0, List.of(), 0);
    }

    @Override
    public Type<HudStatePayload> type() {
        return TYPE;
    }

    public record TagSummary(String stateId, int intensity) {
    }
}
