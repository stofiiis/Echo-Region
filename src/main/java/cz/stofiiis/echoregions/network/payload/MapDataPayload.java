package cz.stofiiis.echoregions.network.payload;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MapDataPayload(
        int centerX,
        int centerZ,
        String dimensionNamespace,
        String dimensionPath,
        int radius,
        List<Cell> cells
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("echoregions", "region_map");
    public static final Type<MapDataPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, Cell> CELL_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Cell::stateId,
            ByteBufCodecs.VAR_INT, Cell::intensity,
            Cell::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MapDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapDataPayload::centerX,
            ByteBufCodecs.VAR_INT, MapDataPayload::centerZ,
            ByteBufCodecs.STRING_UTF8, MapDataPayload::dimensionNamespace,
            ByteBufCodecs.STRING_UTF8, MapDataPayload::dimensionPath,
            ByteBufCodecs.VAR_INT, MapDataPayload::radius,
            CELL_CODEC.apply(ByteBufCodecs.list()), MapDataPayload::cells,
            MapDataPayload::new
    );

    @Override
    public Type<MapDataPayload> type() {
        return TYPE;
    }

    public record Cell(String stateId, int intensity) {
    }
}
