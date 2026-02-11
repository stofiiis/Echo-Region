package cz.stofiiis.echoregions.debug;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

public final class DebugOverlayTracker {
    private static final Map<UUID, DebugState> STATES = new ConcurrentHashMap<>();

    private DebugOverlayTracker() {
    }

    public static void setHudEnabled(ServerPlayer player, boolean enabled) {
        DebugState state = getOrCreate(player);
        state.hudEnabled = enabled;
        cleanupIfEmpty(player.getUUID(), state);
    }

    public static void openMap(ServerPlayer player, int radius) {
        DebugState state = getOrCreate(player);
        state.mapOpen = true;
        state.mapRadius = Math.max(0, radius);
    }

    public static void closeMap(ServerPlayer player) {
        DebugState state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        state.mapOpen = false;
        cleanupIfEmpty(player.getUUID(), state);
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static DebugState get(ServerPlayer player) {
        return STATES.get(player.getUUID());
    }

    private static DebugState getOrCreate(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new DebugState());
    }

    private static void cleanupIfEmpty(UUID playerId, DebugState state) {
        if (!state.hudEnabled && !state.mapOpen) {
            STATES.remove(playerId);
        }
    }

    public static final class DebugState {
        private boolean hudEnabled;
        private boolean mapOpen;
        private int mapRadius;

        public boolean isHudEnabled() {
            return hudEnabled;
        }

        public boolean isMapOpen() {
            return mapOpen;
        }

        public int getMapRadius() {
            return mapRadius;
        }
    }
}
