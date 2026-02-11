package cz.stofiiis.echoregions.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cz.stofiiis.echoregions.data.RegionMemoryData;
import cz.stofiiis.echoregions.data.RegionPos;
import cz.stofiiis.echoregions.network.EchoRegionsNetwork;
import cz.stofiiis.echoregions.network.payload.HudStatePayload;
import cz.stofiiis.echoregions.network.payload.MapDataPayload;
import cz.stofiiis.echoregions.region.RegionState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class DebugOverlaySync {
    private static final int HUD_UPDATE_INTERVAL_TICKS = 20;
    private static int tickCounter = 0;

    private DebugOverlaySync() {
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter < HUD_UPDATE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DebugOverlayTracker.DebugState state = DebugOverlayTracker.get(player);
            if (state == null) {
                continue;
            }
            if (state.isHudEnabled()) {
                sendHudUpdate(player);
            }
            if (state.isMapOpen()) {
                sendMapUpdate(player);
            }
        }
    }

    public static void sendHudUpdate(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        RegionMemoryData data = RegionMemoryData.get(level);
        RegionPos regionPos = RegionPos.fromChunk(player.chunkPosition());
        long gameTime = level.getGameTime();

        RegionState.AggregatedScores headlineScores = RegionState.aggregateAreaScoresForHeadline(data, regionPos);
        RegionState.HeadlineResult headline = RegionState.resolveHeadline(data, regionPos, headlineScores, gameTime);
        RegionState state = headline.state();
        data.updateHeadline(regionPos, state.getId(), gameTime);

        RegionState.Intensity intensity = RegionState.intensityForState(state, headlineScores);
        List<RegionState.ActiveTag> activeTags = RegionState.getActiveTags(headlineScores).stream()
                .sorted(Comparator.comparingInt(RegionState.ActiveTag::score).reversed())
                .limit(3)
                .toList();
        List<HudStatePayload.TagSummary> summaries = new ArrayList<>();
        for (RegionState.ActiveTag tag : activeTags) {
            summaries.add(new HudStatePayload.TagSummary(tag.state().getId(), tag.intensity().ordinal()));
        }

        Identifier dimension = level.dimension().identifier();
        int totalWeight = RegionState.totalScore(headlineScores);
        HudStatePayload payload = new HudStatePayload(
                true,
                regionPos.x(),
                regionPos.z(),
                dimension.getNamespace(),
                dimension.getPath(),
                state.getId(),
                intensity.ordinal(),
                summaries,
                totalWeight
        );
        EchoRegionsNetwork.sendHudState(player, payload);
    }

    public static void sendHudDisabled(ServerPlayer player) {
        HudStatePayload payload = HudStatePayload.disabled();
        EchoRegionsNetwork.sendHudState(player, payload);
    }

    public static void sendMapUpdate(ServerPlayer player) {
        DebugOverlayTracker.DebugState state = DebugOverlayTracker.get(player);
        if (state == null || !state.isMapOpen()) {
            return;
        }
        int radius = state.getMapRadius();
        ServerLevel level = (ServerLevel) player.level();
        RegionMemoryData data = RegionMemoryData.get(level);
        RegionPos center = RegionPos.fromChunk(player.chunkPosition());
        long gameTime = level.getGameTime();
        int size = radius * 2 + 1;
        List<MapDataPayload.Cell> cells = new ArrayList<>(size * size);
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                RegionPos pos = new RegionPos(center.x() + dx, center.z() + dz);
                RegionState.AggregatedScores scores = RegionState.aggregateAreaScoresForHeadline(data, pos);
                RegionState.HeadlineResult headline = RegionState.resolveHeadline(data, pos, scores, gameTime);
                RegionState cellState = headline.state();
                data.updateHeadline(pos, cellState.getId(), gameTime);
                RegionState.Intensity intensity = RegionState.intensityForState(cellState, scores);
                cells.add(new MapDataPayload.Cell(cellState.getId(), intensity.ordinal()));
            }
        }
        Identifier dimension = level.dimension().identifier();
        MapDataPayload payload = new MapDataPayload(
                center.x(),
                center.z(),
                dimension.getNamespace(),
                dimension.getPath(),
                radius,
                cells
        );
        EchoRegionsNetwork.sendMapData(player, payload);
    }
}
