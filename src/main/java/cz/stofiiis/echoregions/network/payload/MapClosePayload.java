package cz.stofiiis.echoregions.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MapClosePayload(boolean closed) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("echoregions", "map_close");
    public static final Type<MapClosePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MapClosePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MapClosePayload::closed,
            MapClosePayload::new
    );

    @Override
    public Type<MapClosePayload> type() {
        return TYPE;
    }
}
