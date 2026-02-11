package cz.stofiiis.echoregions.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import cz.stofiiis.echoregions.EchoRegions;
import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.server.level.ServerLevel;

public class RegionMemoryData extends SavedData {
    public static final String DATA_ID = EchoRegions.MOD_ID + "_region_memory";
    public static final int DATA_VERSION = 2;

    private static final Codec<Map<Long, RegionMemory>> REGION_MAP_CODEC = Codec.unboundedMap(Codec.STRING, RegionMemory.CODEC)
            .xmap(RegionMemoryData::stringKeyMapToLong, RegionMemoryData::longKeyMapToString);
    public static final Codec<RegionMemoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("dataVersion", 0).forGetter(data -> data.dataVersion),
            REGION_MAP_CODEC.optionalFieldOf("regions", Map.<Long, RegionMemory>of()).forGetter(data -> data.regions),
            REGION_MAP_CODEC.optionalFieldOf("memories", Map.<Long, RegionMemory>of()).forGetter(data -> Map.of())
    ).apply(instance, RegionMemoryData::new));

    public static final SavedDataType<RegionMemoryData> TYPE = new SavedDataType<>(
            DATA_ID,
            RegionMemoryData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<Long, RegionMemory> regions;
    private final int dataVersion;

    public RegionMemoryData() {
        this(DATA_VERSION, new HashMap<>(), Map.of());
    }

    private RegionMemoryData(int dataVersion, Map<Long, RegionMemory> regions, Map<Long, RegionMemory> legacyMemories) {
        boolean dirty = false;
        this.dataVersion = DATA_VERSION;
        if (!regions.isEmpty()) {
            this.regions = new HashMap<>(regions);
        } else if (!legacyMemories.isEmpty()) {
            MigrationResult migration = migrateLegacyChunks(legacyMemories);
            this.regions = migration.regions();
            dirty = true;
            EchoRegions.LOGGER.info("Migrated {} chunk entries into {} region entries.", migration.chunkCount(), migration.regionCount());
        } else {
            this.regions = new HashMap<>();
        }
        if (normalizeMemories(this.regions)) {
            dirty = true;
        }
        if (dataVersion != DATA_VERSION) {
            dirty = true;
        }
        if (dirty) {
            setDirty();
        }
    }

    public static RegionMemoryData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public RegionMemory getMemory(RegionPos pos) {
        return regions.get(pos.toLong());
    }

    public boolean updateHeadline(RegionPos pos, String headlineId, long gameTime) {
        RegionMemory memory = regions.get(pos.toLong());
        if (memory == null) {
            return false;
        }
        if (memory.setHeadlineId(headlineId, gameTime)) {
            setDirty();
            return true;
        }
        return false;
    }

    public void addMiningScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addMining(amount, gameTime);
        setDirty();
    }

    public void addCombatScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addCombat(amount, gameTime);
        setDirty();
    }

    public void addDeathScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addDeath(amount, gameTime);
        setDirty();
    }

    public void addFarmScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addFarm(amount, gameTime);
        setDirty();
    }

    public void addBuildScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addBuild(amount, gameTime);
        setDirty();
    }

    public void addFireScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addFire(amount, gameTime);
        setDirty();
    }

    public void addTravelScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addTravel(amount, gameTime);
        setDirty();
    }

    public void addExploitScore(RegionPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addExploit(amount, gameTime);
        setDirty();
    }

    public void decayAllFlat(int negativeAmount, int positiveAmount, long gameTime) {
        boolean residualEnabled = EchoRegionsConfig.RESIDUAL_ENABLED.get();
        double residualNegativePercent = EchoRegionsConfig.RESIDUAL_NEGATIVE_PERCENT.get();
        double residualPositivePercent = EchoRegionsConfig.RESIDUAL_POSITIVE_PERCENT.get();
        int residualMinFloor = EchoRegionsConfig.RESIDUAL_MIN_FLOOR.get();
        decayAllInternal(memory -> memory.decayFlat(
                negativeAmount,
                positiveAmount,
                residualEnabled,
                residualNegativePercent,
                residualPositivePercent,
                residualMinFloor,
                gameTime
        ));
    }

    public void decayAllPercent(double negativeFactor, double positiveFactor, long gameTime) {
        boolean residualEnabled = EchoRegionsConfig.RESIDUAL_ENABLED.get();
        double residualNegativePercent = EchoRegionsConfig.RESIDUAL_NEGATIVE_PERCENT.get();
        double residualPositivePercent = EchoRegionsConfig.RESIDUAL_POSITIVE_PERCENT.get();
        int residualMinFloor = EchoRegionsConfig.RESIDUAL_MIN_FLOOR.get();
        decayAllInternal(memory -> memory.decayPercent(
                negativeFactor,
                positiveFactor,
                residualEnabled,
                residualNegativePercent,
                residualPositivePercent,
                residualMinFloor,
                gameTime
        ));
    }

    private void decayAllInternal(Function<RegionMemory, Boolean> decayer) {
        boolean changed = false;
        Iterator<Map.Entry<Long, RegionMemory>> iterator = regions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, RegionMemory> entry = iterator.next();
            RegionMemory memory = entry.getValue();
            if (decayer.apply(memory)) {
                changed = true;
            }
            if (memory.isEmpty() && memory.isHistoryEmpty()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    private RegionMemory getOrCreate(RegionPos pos) {
        long key = pos.toLong();
        RegionMemory memory = regions.get(key);
        if (memory == null) {
            memory = new RegionMemory();
            regions.put(key, memory);
        }
        return memory;
    }

    private static Map<Long, RegionMemory> stringKeyMapToLong(Map<String, RegionMemory> input) {
        Map<Long, RegionMemory> output = new HashMap<>();
        for (Map.Entry<String, RegionMemory> entry : input.entrySet()) {
            try {
                output.put(Long.parseLong(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {
                // Skip malformed keys to avoid crashing on bad data.
            }
        }
        return output;
    }

    private static Map<String, RegionMemory> longKeyMapToString(Map<Long, RegionMemory> input) {
        Map<String, RegionMemory> output = new HashMap<>();
        for (Map.Entry<Long, RegionMemory> entry : input.entrySet()) {
            output.put(Long.toString(entry.getKey()), entry.getValue());
        }
        return output;
    }

    private static boolean normalizeMemories(Map<Long, RegionMemory> regions) {
        boolean changed = false;
        for (RegionMemory memory : regions.values()) {
            if (memory.normalize()) {
                changed = true;
            }
        }
        return changed;
    }

    private static MigrationResult migrateLegacyChunks(Map<Long, RegionMemory> legacyMemories) {
        Map<Long, RegionMemory> aggregated = new HashMap<>();
        Map<Long, Aggregate> accumulators = new HashMap<>();
        int chunkCount = 0;
        for (Map.Entry<Long, RegionMemory> entry : legacyMemories.entrySet()) {
            chunkCount++;
            long chunkKey = entry.getKey();
            int chunkX = (int) (chunkKey >> 32);
            int chunkZ = (int) chunkKey;
            RegionPos regionPos = RegionPos.fromChunk(chunkX, chunkZ);
            long regionKey = regionPos.toLong();
            Aggregate agg = accumulators.computeIfAbsent(regionKey, key -> new Aggregate());
            RegionMemory memory = entry.getValue();
            agg.mining += memory.getMiningScore();
            agg.combat += memory.getCombatScore();
            agg.death += memory.getDeathScore();
            agg.farm += memory.getFarmScore();
            agg.build += memory.getBuildScore();
            agg.fire += memory.getFireScore();
            agg.travel += memory.getTravelScore();
            agg.exploit += memory.getExploitScore();
            agg.lastUpdated = Math.max(agg.lastUpdated, memory.getLastUpdated());
        }
        for (Map.Entry<Long, Aggregate> entry : accumulators.entrySet()) {
            Aggregate agg = entry.getValue();
            aggregated.put(entry.getKey(), new RegionMemory(
                    agg.mining,
                    agg.combat,
                    agg.death,
                    agg.farm,
                    agg.build,
                    agg.fire,
                    agg.travel,
                    agg.exploit,
                    agg.lastUpdated
            ));
        }
        return new MigrationResult(aggregated, chunkCount, aggregated.size());
    }

    private record MigrationResult(Map<Long, RegionMemory> regions, int chunkCount, int regionCount) {
    }

    private static final class Aggregate {
        private int mining;
        private int combat;
        private int death;
        private int farm;
        private int build;
        private int fire;
        private int travel;
        private int exploit;
        private long lastUpdated;
    }
}
