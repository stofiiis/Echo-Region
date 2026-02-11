package cz.stofiiis.echoregions.client;

import cz.stofiiis.echoregions.network.payload.HudStatePayload;
import cz.stofiiis.echoregions.network.payload.MapDataPayload;

public final class ClientDebugData {
    private static volatile HudStatePayload hudPayload = HudStatePayload.disabled();
    private static volatile MapDataPayload mapPayload;

    private ClientDebugData() {
    }

    public static void updateHud(HudStatePayload payload) {
        hudPayload = payload;
    }

    public static HudStatePayload getHudPayload() {
        return hudPayload;
    }

    public static void updateMap(MapDataPayload payload) {
        mapPayload = payload;
    }

    public static MapDataPayload getMapPayload() {
        return mapPayload;
    }

    public static void clearMap() {
        mapPayload = null;
    }
}
