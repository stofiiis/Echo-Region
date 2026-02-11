package cz.stofiiis.echoregions.network;

import cz.stofiiis.echoregions.debug.DebugOverlayTracker;
import cz.stofiiis.echoregions.network.payload.HudStatePayload;
import cz.stofiiis.echoregions.network.payload.MapClosePayload;
import cz.stofiiis.echoregions.network.payload.MapDataPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EchoRegionsNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private EchoRegionsNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(HudStatePayload.TYPE, HudStatePayload.STREAM_CODEC);
        registrar.playToClient(MapDataPayload.TYPE, MapDataPayload.STREAM_CODEC);
        registrar.playToServer(MapClosePayload.TYPE, MapClosePayload.STREAM_CODEC, EchoRegionsNetwork::handleMapClose);
    }

    public static void sendHudState(ServerPlayer player, HudStatePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendMapData(ServerPlayer player, MapDataPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void handleMapClose(MapClosePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            DebugOverlayTracker.closeMap(player);
        }
    }
}
