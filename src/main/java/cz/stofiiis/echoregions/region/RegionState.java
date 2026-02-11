package cz.stofiiis.echoregions.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import cz.stofiiis.echoregions.data.RegionMemory;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import cz.stofiiis.echoregions.data.RegionPos;

public enum RegionState {
    NEUTRAL("neutral"),
    SCARRED("scarred"),
    HAUNTED("haunted"),
    WAR_TORN("war_torn"),
    CULTIVATED("cultivated"),
    SETTLED("settled"),
    BLIGHTED("blighted"),
    TRAVELLED("travelled"),
    EXPLOITED("exploited");

    private final String id;

    RegionState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static RegionState getState(AggregatedScores scores) {
        DominantScore dominant = getDominant(scores);
        if (dominant == DominantScore.NONE) {
            return NEUTRAL;
        }
        int dominantScore = scoreFor(dominant, scores);
        int threshold = thresholdFor(dominant);
        if (dominantScore < threshold) {
            return NEUTRAL;
        }
        return toRegionState(dominant);
    }

    public static HeadlineResult resolveHeadline(RegionMemoryData data, RegionPos center, AggregatedScores scores, long gameTime) {
        RegionMemory memory = data.getMemory(center);
        RegionState previous = memory != null ? fromId(memory.getHeadlineId()) : null;
        RegionState candidate = getState(scores);
        DominantScore candidateScoreType = dominantForState(candidate);
        int candidateScore = scoreFor(candidateScoreType, scores);

        if (previous == null || previous == NEUTRAL) {
            String reason = "changed: no previous (candidateScore="
                    + candidateScore
                    + ", threshold=" + thresholdFor(candidateScoreType) + ")";
            return new HeadlineResult(candidate, reason, false);
        }

        if (candidate == previous) {
            int currentScore = scoreFor(dominantForState(previous), scores);
            String reason = "kept: same state (current=" + currentScore + ")";
            return new HeadlineResult(previous, reason, true);
        }

        long minDurationTicks = Math.max(0L, EchoRegionsConfig.MIN_HEADLINE_DURATION_MINUTES.get()) * 20L * 60L;
        long headlineUpdated = memory != null ? memory.getHeadlineUpdated() : 0L;
        if (minDurationTicks > 0 && headlineUpdated > 0L && gameTime - headlineUpdated < minDurationTicks) {
            long remaining = minDurationTicks - Math.max(0L, gameTime - headlineUpdated);
            String reason = "kept: minDuration (" + remaining + " ticks)";
            return new HeadlineResult(previous, reason, true);
        }

        DominantScore previousScore = dominantForState(previous);
        int currentScore = scoreFor(previousScore, scores);
        int threshold = thresholdFor(previousScore);
        double keepFloor = threshold * EchoRegionsConfig.HEADLINE_KEEP_FACTOR.get();
        double switchTrigger = currentScore * EchoRegionsConfig.HEADLINE_SWITCH_RATIO.get();

        boolean canSwitch = candidateScore >= switchTrigger && currentScore < keepFloor;
        RegionState headline = canSwitch ? candidate : previous;
        String reason = String.format(
                Locale.ROOT,
                "%s: current=%d, keep<%.2f, candidate=%d, switch>=%.2f",
                canSwitch ? "changed" : "kept",
                currentScore,
                keepFloor,
                candidateScore,
                switchTrigger
        );
        return new HeadlineResult(headline, reason, !canSwitch);
    }

    public static DominantScore getDominant(AggregatedScores scores) {
        return getDominant(
                scores.mining(),
                scores.combat(),
                scores.death(),
                scores.farm(),
                scores.build(),
                scores.fire(),
                scores.travel(),
                scores.exploit()
        );
    }

    public static AggregatedScores localScores(RegionMemoryData data, RegionPos center) {
        RegionMemory memory = data.getMemory(center);
        if (memory == null) {
            return new AggregatedScores(0, 0, 0, 0, 0, 0, 0, 0);
        }
        return new AggregatedScores(
                memory.getMiningScore(),
                memory.getCombatScore(),
                memory.getDeathScore(),
                memory.getFarmScore(),
                memory.getBuildScore(),
                memory.getFireScore(),
                memory.getTravelScore(),
                memory.getExploitScore()
        );
    }

    public static AggregatedScores aggregateAreaScores(RegionMemoryData data, RegionPos center) {
        return aggregateScores(data, center, 1);
    }

    public static AggregatedScores aggregateScores(RegionMemoryData data, RegionPos center, int radius) {
        int mining = 0;
        int combat = 0;
        int death = 0;
        int farm = 0;
        int build = 0;
        int fire = 0;
        int travel = 0;
        int exploit = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                RegionPos pos = new RegionPos(center.x() + dx, center.z() + dz);
                RegionMemory memory = data.getMemory(pos);
                if (memory == null) {
                    continue;
                }
                mining += memory.getMiningScore();
                combat += memory.getCombatScore();
                death += memory.getDeathScore();
                farm += memory.getFarmScore();
                build += memory.getBuildScore();
                fire += memory.getFireScore();
                travel += memory.getTravelScore();
                exploit += memory.getExploitScore();
            }
        }
        return new AggregatedScores(mining, combat, death, farm, build, fire, travel, exploit);
    }

    public static AggregatedScores aggregateAreaMaxScores(RegionMemoryData data, RegionPos center) {
        return aggregateMaxScores(data, center, 1);
    }

    public static AggregatedScores aggregateAreaFloorScores(RegionMemoryData data, RegionPos center) {
        return aggregateFloorScores(data, center, 1);
    }

    public static AggregatedScores aggregateAreaScoresForHeadline(RegionMemoryData data, RegionPos center) {
        AggregatedScores current = aggregateAreaScores(data, center);
        if (EchoRegionsConfig.RESIDUAL_AFFECTS_HEADLINE.get()) {
            return current;
        }
        AggregatedScores floors = aggregateAreaFloorScores(data, center);
        return subtractScores(current, floors);
    }

    private static AggregatedScores aggregateMaxScores(RegionMemoryData data, RegionPos center, int radius) {
        int mining = 0;
        int combat = 0;
        int death = 0;
        int farm = 0;
        int build = 0;
        int fire = 0;
        int travel = 0;
        int exploit = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                RegionPos pos = new RegionPos(center.x() + dx, center.z() + dz);
                RegionMemory memory = data.getMemory(pos);
                if (memory == null) {
                    continue;
                }
                mining += memory.getMaxMiningScore();
                combat += memory.getMaxCombatScore();
                death += memory.getMaxDeathScore();
                farm += memory.getMaxFarmScore();
                build += memory.getMaxBuildScore();
                fire += memory.getMaxFireScore();
                travel += memory.getMaxTravelScore();
                exploit += memory.getMaxExploitScore();
            }
        }
        return new AggregatedScores(mining, combat, death, farm, build, fire, travel, exploit);
    }

    private static AggregatedScores aggregateFloorScores(RegionMemoryData data, RegionPos center, int radius) {
        boolean residualEnabled = EchoRegionsConfig.RESIDUAL_ENABLED.get();
        double negativePercent = EchoRegionsConfig.RESIDUAL_NEGATIVE_PERCENT.get();
        double positivePercent = EchoRegionsConfig.RESIDUAL_POSITIVE_PERCENT.get();
        int minFloor = EchoRegionsConfig.RESIDUAL_MIN_FLOOR.get();

        int mining = 0;
        int combat = 0;
        int death = 0;
        int farm = 0;
        int build = 0;
        int fire = 0;
        int travel = 0;
        int exploit = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                RegionPos pos = new RegionPos(center.x() + dx, center.z() + dz);
                RegionMemory memory = data.getMemory(pos);
                if (memory == null) {
                    continue;
                }
                mining += memory.floorMining(residualEnabled, negativePercent, minFloor);
                combat += memory.floorCombat(residualEnabled, negativePercent, minFloor);
                death += memory.floorDeath(residualEnabled, negativePercent, minFloor);
                fire += memory.floorFire(residualEnabled, negativePercent, minFloor);
                exploit += memory.floorExploit(residualEnabled, negativePercent, minFloor);
                farm += memory.floorFarm(residualEnabled, positivePercent, minFloor);
                build += memory.floorBuild(residualEnabled, positivePercent, minFloor);
                travel += memory.floorTravel(residualEnabled, positivePercent, minFloor);
            }
        }
        return new AggregatedScores(mining, combat, death, farm, build, fire, travel, exploit);
    }

    private static DominantScore getDominant(
            int mining,
            int combat,
            int death,
            int farm,
            int build,
            int fire,
            int travel,
            int exploit
    ) {
        int max = Math.max(
                Math.max(Math.max(mining, combat), Math.max(death, farm)),
                Math.max(Math.max(build, fire), Math.max(travel, exploit))
        );
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
        if (farm == max) {
            return DominantScore.FARM;
        }
        if (build == max) {
            return DominantScore.BUILD;
        }
        if (fire == max) {
            return DominantScore.FIRE;
        }
        if (travel == max) {
            return DominantScore.TRAVEL;
        }
        if (exploit == max) {
            return DominantScore.EXPLOIT;
        }
        return DominantScore.NONE;
    }

    public static List<ActiveTag> getActiveTags(AggregatedScores scores) {
        List<ActiveTag> tags = new ArrayList<>();
        for (DominantScore score : DominantScore.values()) {
            if (score == DominantScore.NONE) {
                continue;
            }
            int value = scoreFor(score, scores);
            int threshold = thresholdFor(score);
            if (threshold <= 0 ? value > 0 : value >= threshold) {
                tags.add(new ActiveTag(toRegionState(score), intensityForScore(value, threshold), value));
            }
        }
        return tags;
    }

    public static List<TagSnapshot> buildTagSnapshots(AggregatedScores current, AggregatedScores max, AggregatedScores floor) {
        List<TagSnapshot> tags = new ArrayList<>();
        for (DominantScore score : DominantScore.values()) {
            if (score == DominantScore.NONE) {
                continue;
            }
            int currentScore = scoreFor(score, current);
            if (currentScore <= 0) {
                continue;
            }
            int maxScore = scoreFor(score, max);
            int floorScore = scoreFor(score, floor);
            int threshold = thresholdFor(score);
            tags.add(new TagSnapshot(
                    toRegionState(score),
                    intensityForScore(currentScore, threshold),
                    currentScore,
                    maxScore,
                    floorScore
            ));
        }
        return tags;
    }

    public static Intensity intensityForState(RegionState state, AggregatedScores scores) {
        if (state == null || state == NEUTRAL) {
            return Intensity.LOW;
        }
        DominantScore dominant = dominantForState(state);
        return intensityForScore(scoreFor(dominant, scores), thresholdFor(dominant));
    }

    public enum DominantScore {
        NONE("none"),
        MINING("mining"),
        COMBAT("combat"),
        DEATH("death"),
        FARM("farm"),
        BUILD("build"),
        FIRE("fire"),
        TRAVEL("travel"),
        EXPLOIT("exploit");

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

    public static int getThresholdBuild() {
        return EchoRegionsConfig.THRESHOLD_BUILD.get();
    }

    public static int getThresholdFire() {
        return EchoRegionsConfig.THRESHOLD_FIRE.get();
    }

    public static int getThresholdTravel() {
        return EchoRegionsConfig.THRESHOLD_TRAVEL.get();
    }

    public static int getThresholdExploit() {
        return EchoRegionsConfig.THRESHOLD_EXPLOIT.get();
    }

    public static double getHeadlineKeepFactor() {
        return EchoRegionsConfig.HEADLINE_KEEP_FACTOR.get();
    }

    public static double getHeadlineSwitchRatio() {
        return EchoRegionsConfig.HEADLINE_SWITCH_RATIO.get();
    }

    private static int scoreFor(DominantScore score, AggregatedScores scores) {
        return switch (score) {
            case MINING -> scores.mining();
            case COMBAT -> scores.combat();
            case DEATH -> scores.death();
            case FARM -> scores.farm();
            case BUILD -> scores.build();
            case FIRE -> scores.fire();
            case TRAVEL -> scores.travel();
            case EXPLOIT -> scores.exploit();
            case NONE -> 0;
        };
    }

    private static int thresholdFor(DominantScore score) {
        return switch (score) {
            case MINING -> getThresholdMining();
            case COMBAT -> getThresholdCombat();
            case DEATH -> getThresholdDeath();
            case FARM -> getThresholdFarm();
            case BUILD -> getThresholdBuild();
            case FIRE -> getThresholdFire();
            case TRAVEL -> getThresholdTravel();
            case EXPLOIT -> getThresholdExploit();
            case NONE -> Integer.MAX_VALUE;
        };
    }

    private static DominantScore dominantForState(RegionState state) {
        return switch (state) {
            case SCARRED -> DominantScore.MINING;
            case WAR_TORN -> DominantScore.COMBAT;
            case HAUNTED -> DominantScore.DEATH;
            case CULTIVATED -> DominantScore.FARM;
            case SETTLED -> DominantScore.BUILD;
            case BLIGHTED -> DominantScore.FIRE;
            case TRAVELLED -> DominantScore.TRAVEL;
            case EXPLOITED -> DominantScore.EXPLOIT;
            case NEUTRAL -> DominantScore.NONE;
        };
    }

    private static RegionState toRegionState(DominantScore dominant) {
        return switch (dominant) {
            case MINING -> SCARRED;
            case COMBAT -> WAR_TORN;
            case DEATH -> HAUNTED;
            case FARM -> CULTIVATED;
            case BUILD -> SETTLED;
            case FIRE -> BLIGHTED;
            case TRAVEL -> TRAVELLED;
            case EXPLOIT -> EXPLOITED;
            case NONE -> NEUTRAL;
        };
    }

    public static Intensity intensityForScore(int score, int threshold) {
        if (threshold <= 0) {
            return Intensity.HIGH;
        }
        long high = (long) threshold * 3L;
        long med = (long) threshold * 2L;
        if (score >= high) {
            return Intensity.HIGH;
        }
        if (score >= med) {
            return Intensity.MED;
        }
        return Intensity.LOW;
    }

    public static int totalScore(AggregatedScores scores) {
        return scores.mining()
                + scores.combat()
                + scores.death()
                + scores.farm()
                + scores.build()
                + scores.fire()
                + scores.travel()
                + scores.exploit();
    }

    public static RegionState fromId(String id) {
        if (id == null) {
            return null;
        }
        for (RegionState state : values()) {
            if (state.id.equals(id)) {
                return state;
            }
        }
        return null;
    }

    public enum Intensity {
        LOW,
        MED,
        HIGH
    }

    public record ActiveTag(RegionState state, Intensity intensity, int score) {
    }

    public record TagSnapshot(RegionState state, Intensity intensity, int current, int max, int floor) {
    }

    public record HeadlineResult(RegionState state, String reason, boolean kept) {
    }

    public record AggregatedScores(
            int mining,
            int combat,
            int death,
            int farm,
            int build,
            int fire,
            int travel,
            int exploit
    ) {
    }

    private static AggregatedScores subtractScores(AggregatedScores current, AggregatedScores floor) {
        return new AggregatedScores(
                Math.max(0, current.mining() - floor.mining()),
                Math.max(0, current.combat() - floor.combat()),
                Math.max(0, current.death() - floor.death()),
                Math.max(0, current.farm() - floor.farm()),
                Math.max(0, current.build() - floor.build()),
                Math.max(0, current.fire() - floor.fire()),
                Math.max(0, current.travel() - floor.travel()),
                Math.max(0, current.exploit() - floor.exploit())
        );
    }
}
