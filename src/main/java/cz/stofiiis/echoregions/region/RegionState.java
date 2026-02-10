package cz.stofiiis.echoregions.region;

import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import cz.stofiiis.echoregions.data.RegionMemory;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import net.minecraft.world.level.ChunkPos;

public enum RegionState {
    NEUTRAL("neutral"),
    SCARRED("scarred"),
    HAUNTED("haunted"),
    WAR_TORN("war_torn"),
    CULTIVATED("cultivated");

    private final String id;

    RegionState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static RegionState getState(RegionMemoryData data, ChunkPos center) {
        AggregatedScores scores = aggregateScores(data, center);
        return getState(scores);
    }

    public static RegionState getState(AggregatedScores scores) {
        DominantScore dominant = getDominant(scores);
        if (dominant == DominantScore.NONE) {
            return NEUTRAL;
        }
        int dominantScore = switch (dominant) {
            case MINING -> scores.mining();
            case COMBAT -> scores.combat();
            case DEATH -> scores.death();
            case FARM -> scores.farm();
            case NONE -> 0;
        };
        int threshold = switch (dominant) {
            case MINING -> getThresholdMining();
            case COMBAT -> getThresholdCombat();
            case DEATH -> getThresholdDeath();
            case FARM -> getThresholdFarm();
            case NONE -> Integer.MAX_VALUE;
        };
        if (dominantScore < threshold) {
            return NEUTRAL;
        }
        return switch (dominant) {
            case MINING -> SCARRED;
            case COMBAT -> WAR_TORN;
            case DEATH -> HAUNTED;
            case FARM -> CULTIVATED;
            case NONE -> NEUTRAL;
        };
    }

    public static DominantScore getDominant(RegionMemoryData data, ChunkPos center) {
        return getDominant(aggregateScores(data, center));
    }

    public static DominantScore getDominant(AggregatedScores scores) {
        return getDominant(scores.mining(), scores.combat(), scores.death(), scores.farm());
    }

    public static AggregatedScores aggregateScores(RegionMemoryData data, ChunkPos center) {
        int mining = 0;
        int combat = 0;
        int death = 0;
        int farm = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                RegionMemory memory = data.getMemory(pos);
                if (memory == null) {
                    continue;
                }
                mining += memory.getMiningScore();
                combat += memory.getCombatScore();
                death += memory.getDeathScore();
                farm += memory.getFarmScore();
            }
        }
        return new AggregatedScores(mining, combat, death, farm);
    }

    private static DominantScore getDominant(int mining, int combat, int death, int farm) {
        if (farm > mining && farm > combat && farm > death) {
            return DominantScore.FARM;
        }
        int max = Math.max(mining, Math.max(combat, death));
        if (max <= 0) {
            return DominantScore.NONE;
        }
        if (death == max) {
            return DominantScore.DEATH;
        }
        if (combat == max) {
            return DominantScore.COMBAT;
        }
        if (mining == max) {
            return DominantScore.MINING;
        }
        return DominantScore.NONE;
    }

    public enum DominantScore {
        NONE("none"),
        MINING("mining"),
        COMBAT("combat"),
        DEATH("death"),
        FARM("farm");

        private final String id;

        DominantScore(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static int getThresholdMining() {
        return EchoRegionsConfig.THRESHOLD_MINING.get();
    }

    public static int getThresholdCombat() {
        return EchoRegionsConfig.THRESHOLD_COMBAT.get();
    }

    public static int getThresholdDeath() {
        return EchoRegionsConfig.THRESHOLD_DEATH.get();
    }

    public static int getThresholdFarm() {
        return EchoRegionsConfig.THRESHOLD_FARM.get();
    }

    public record AggregatedScores(int mining, int combat, int death, int farm) {
    }
}
