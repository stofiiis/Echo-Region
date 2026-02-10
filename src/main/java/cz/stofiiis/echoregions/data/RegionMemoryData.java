package cz.stofiiis.echoregions.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import cz.stofiiis.echoregions.EchoRegions;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.server.level.ServerLevel;

public class RegionMemoryData extends SavedData {
    public static final String DATA_ID = EchoRegions.MOD_ID + "_region_memory";

    private static final Codec<Map<Long, RegionMemory>> MEMORY_MAP_CODEC = Codec.unboundedMap(Codec.STRING, RegionMemory.CODEC)
            .xmap(RegionMemoryData::stringKeyMapToLong, RegionMemoryData::longKeyMapToString);
    public static final Codec<RegionMemoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MEMORY_MAP_CODEC.optionalFieldOf("memories", Map.<Long, RegionMemory>of()).forGetter(data -> data.memories)
    ).apply(instance, RegionMemoryData::new));

    public static final SavedDataType<RegionMemoryData> TYPE = new SavedDataType<>(
            DATA_ID,
            RegionMemoryData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<Long, RegionMemory> memories;

    public RegionMemoryData() {
        this(new HashMap<>());
    }

    private RegionMemoryData(Map<Long, RegionMemory> memories) {
        this.memories = new HashMap<>(memories);
    }

    public static RegionMemoryData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public RegionMemory getMemory(ChunkPos pos) {
        return memories.get(pos.toLong());
    }

    public void addMiningScore(ChunkPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addMining(amount, gameTime);
        setDirty();
    }

    public void addCombatScore(ChunkPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addCombat(amount, gameTime);
        setDirty();
    }

    public void addDeathScore(ChunkPos pos, int amount, long gameTime) {
        RegionMemory memory = getOrCreate(pos);
        memory.addDeath(amount, gameTime);
        setDirty();
    }

    public void decayAll(long gameTime) {
        boolean changed = false;
        Iterator<Map.Entry<Long, RegionMemory>> iterator = memories.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, RegionMemory> entry = iterator.next();
            RegionMemory memory = entry.getValue();
            if (memory.decay(1, gameTime)) {
                changed = true;
            }
            if (memory.isEmpty()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    private RegionMemory getOrCreate(ChunkPos pos) {
        long key = pos.toLong();
        RegionMemory memory = memories.get(key);
        if (memory == null) {
            memory = new RegionMemory();
            memories.put(key, memory);
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
}
