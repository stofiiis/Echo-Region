package cz.tvojeid.echoregions.region;

import cz.tvojeid.echoregions.data.RegionMemory;

public enum RegionState {
    NEUTRAL("neutral"),
    SCARRED("scarred"),
    HAUNTED("haunted"),
    WAR_TORN("war_torn");

    public static final int DEFAULT_THRESHOLD = 10;
    public static final int THRESHOLD_MINING = DEFAULT_THRESHOLD;
    public static final int THRESHOLD_COMBAT = DEFAULT_THRESHOLD;
    public static final int THRESHOLD_DEATH = DEFAULT_THRESHOLD;

    private final String id;

    RegionState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static RegionState getState(RegionMemory memory) {
        if (memory == null) {
            return NEUTRAL;
        }
        int mining = memory.getMiningScore();
        int combat = memory.getCombatScore();
        int death = memory.getDeathScore();
        DominantScore dominant = getDominant(mining, combat, death);
        if (dominant == DominantScore.NONE) {
            return NEUTRAL;
        }
        int dominantScore = switch (dominant) {
            case MINING -> mining;
            case COMBAT -> combat;
            case DEATH -> death;
            case NONE -> 0;
        };
        int threshold = switch (dominant) {
            case MINING -> THRESHOLD_MINING;
            case COMBAT -> THRESHOLD_COMBAT;
            case DEATH -> THRESHOLD_DEATH;
            case NONE -> Integer.MAX_VALUE;
        };
        if (dominantScore < threshold) {
            return NEUTRAL;
        }
        return switch (dominant) {
            case MINING -> SCARRED;
            case COMBAT -> WAR_TORN;
            case DEATH -> HAUNTED;
            case NONE -> NEUTRAL;
        };
    }

    public static DominantScore getDominant(RegionMemory memory) {
        if (memory == null) {
            return DominantScore.NONE;
        }
        return getDominant(memory.getMiningScore(), memory.getCombatScore(), memory.getDeathScore());
    }

    private static DominantScore getDominant(int mining, int combat, int death) {
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
        DEATH("death");

        private final String id;

        DominantScore(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
