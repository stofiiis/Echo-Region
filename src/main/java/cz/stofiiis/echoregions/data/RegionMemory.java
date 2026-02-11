package cz.stofiiis.echoregions.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class RegionMemory {
    private static final Codec<List<Integer>> MAX_LIST_CODEC = Codec.INT.listOf();

    public static final Codec<RegionMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("miningScore", 0).forGetter(RegionMemory::getMiningScore),
            Codec.INT.optionalFieldOf("combatScore", 0).forGetter(RegionMemory::getCombatScore),
            Codec.INT.optionalFieldOf("deathScore", 0).forGetter(RegionMemory::getDeathScore),
            Codec.INT.optionalFieldOf("farmScore", 0).forGetter(RegionMemory::getFarmScore),
            Codec.INT.optionalFieldOf("buildScore", 0).forGetter(RegionMemory::getBuildScore),
            Codec.INT.optionalFieldOf("fireScore", 0).forGetter(RegionMemory::getFireScore),
            Codec.INT.optionalFieldOf("travelScore", 0).forGetter(RegionMemory::getTravelScore),
            Codec.INT.optionalFieldOf("exploitScore", 0).forGetter(RegionMemory::getExploitScore),
            MAX_LIST_CODEC.optionalFieldOf("maxScores", List.of()).forGetter(RegionMemory::getMaxScoresList),
            Codec.LONG.optionalFieldOf("lastUpdated", 0L).forGetter(RegionMemory::getLastUpdated),
            Codec.STRING.optionalFieldOf("headline", "neutral").forGetter(RegionMemory::getHeadlineId),
            Codec.LONG.optionalFieldOf("headlineUpdated", 0L).forGetter(RegionMemory::getHeadlineUpdated)
    ).apply(instance, RegionMemory::new));

    private int miningScore;
    private int combatScore;
    private int deathScore;
    private int farmScore;
    private int buildScore;
    private int fireScore;
    private int travelScore;
    private int exploitScore;
    private int maxMiningScore;
    private int maxCombatScore;
    private int maxDeathScore;
    private int maxFarmScore;
    private int maxBuildScore;
    private int maxFireScore;
    private int maxTravelScore;
    private int maxExploitScore;
    private long lastUpdated;
    private String headlineId;
    private long headlineUpdated;

    public RegionMemory() {
        this(0, 0, 0, 0, 0, 0, 0, 0, List.of(), 0L, "neutral", 0L);
    }

    public RegionMemory(
            int miningScore,
            int combatScore,
            int deathScore,
            int farmScore,
            int buildScore,
            int fireScore,
            int travelScore,
            int exploitScore,
            long lastUpdated
    ) {
        this(
                miningScore,
                combatScore,
                deathScore,
                farmScore,
                buildScore,
                fireScore,
                travelScore,
                exploitScore,
                List.of(
                        miningScore,
                        combatScore,
                        deathScore,
                        farmScore,
                        buildScore,
                        fireScore,
                        travelScore,
                        exploitScore
                ),
                lastUpdated,
                "neutral",
                lastUpdated
        );
    }

    public RegionMemory(
            int miningScore,
            int combatScore,
            int deathScore,
            int farmScore,
            int buildScore,
            int fireScore,
            int travelScore,
            int exploitScore,
            List<Integer> maxScores,
            long lastUpdated,
            String headlineId,
            long headlineUpdated
    ) {
        this.miningScore = Math.max(0, miningScore);
        this.combatScore = Math.max(0, combatScore);
        this.deathScore = Math.max(0, deathScore);
        this.farmScore = Math.max(0, farmScore);
        this.buildScore = Math.max(0, buildScore);
        this.fireScore = Math.max(0, fireScore);
        this.travelScore = Math.max(0, travelScore);
        this.exploitScore = Math.max(0, exploitScore);
        this.maxMiningScore = Math.max(this.miningScore, maxFromList(maxScores, 0, this.miningScore));
        this.maxCombatScore = Math.max(this.combatScore, maxFromList(maxScores, 1, this.combatScore));
        this.maxDeathScore = Math.max(this.deathScore, maxFromList(maxScores, 2, this.deathScore));
        this.maxFarmScore = Math.max(this.farmScore, maxFromList(maxScores, 3, this.farmScore));
        this.maxBuildScore = Math.max(this.buildScore, maxFromList(maxScores, 4, this.buildScore));
        this.maxFireScore = Math.max(this.fireScore, maxFromList(maxScores, 5, this.fireScore));
        this.maxTravelScore = Math.max(this.travelScore, maxFromList(maxScores, 6, this.travelScore));
        this.maxExploitScore = Math.max(this.exploitScore, maxFromList(maxScores, 7, this.exploitScore));
        this.lastUpdated = Math.max(0L, lastUpdated);
        this.headlineId = headlineId == null ? "neutral" : headlineId;
        this.headlineUpdated = Math.max(0L, headlineUpdated);
    }

    public int getMiningScore() {
        return miningScore;
    }

    public int getCombatScore() {
        return combatScore;
    }

    public int getDeathScore() {
        return deathScore;
    }

    public int getFarmScore() {
        return farmScore;
    }

    public int getBuildScore() {
        return buildScore;
    }

    public int getFireScore() {
        return fireScore;
    }

    public int getTravelScore() {
        return travelScore;
    }

    public int getExploitScore() {
        return exploitScore;
    }

    public int getMaxMiningScore() {
        return maxMiningScore;
    }

    public int getMaxCombatScore() {
        return maxCombatScore;
    }

    public int getMaxDeathScore() {
        return maxDeathScore;
    }

    public int getMaxFarmScore() {
        return maxFarmScore;
    }

    public int getMaxBuildScore() {
        return maxBuildScore;
    }

    public int getMaxFireScore() {
        return maxFireScore;
    }

    public int getMaxTravelScore() {
        return maxTravelScore;
    }

    public int getMaxExploitScore() {
        return maxExploitScore;
    }

    private List<Integer> getMaxScoresList() {
        return List.of(
                maxMiningScore,
                maxCombatScore,
                maxDeathScore,
                maxFarmScore,
                maxBuildScore,
                maxFireScore,
                maxTravelScore,
                maxExploitScore
        );
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public String getHeadlineId() {
        return headlineId;
    }

    public long getHeadlineUpdated() {
        return headlineUpdated;
    }

    boolean setHeadlineId(String headlineId, long gameTime) {
        String next = headlineId == null ? "neutral" : headlineId;
        if (next.equals(this.headlineId)) {
            return false;
        }
        this.headlineId = next;
        this.headlineUpdated = Math.max(0L, gameTime);
        return true;
    }

    void addMining(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        miningScore += amount;
        if (miningScore > maxMiningScore) {
            maxMiningScore = miningScore;
        }
        lastUpdated = gameTime;
    }

    void addCombat(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        combatScore += amount;
        if (combatScore > maxCombatScore) {
            maxCombatScore = combatScore;
        }
        lastUpdated = gameTime;
    }

    void addDeath(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        deathScore += amount;
        if (deathScore > maxDeathScore) {
            maxDeathScore = deathScore;
        }
        lastUpdated = gameTime;
    }

    void addFarm(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        farmScore += amount;
        if (farmScore > maxFarmScore) {
            maxFarmScore = farmScore;
        }
        lastUpdated = gameTime;
    }

    void addBuild(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        buildScore += amount;
        if (buildScore > maxBuildScore) {
            maxBuildScore = buildScore;
        }
        lastUpdated = gameTime;
    }

    void addFire(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        fireScore += amount;
        if (fireScore > maxFireScore) {
            maxFireScore = fireScore;
        }
        lastUpdated = gameTime;
    }

    void addTravel(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        travelScore += amount;
        if (travelScore > maxTravelScore) {
            maxTravelScore = travelScore;
        }
        lastUpdated = gameTime;
    }

    void addExploit(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        exploitScore += amount;
        if (exploitScore > maxExploitScore) {
            maxExploitScore = exploitScore;
        }
        lastUpdated = gameTime;
    }

    boolean decayFlat(
            int negativeAmount,
            int positiveAmount,
            boolean residualEnabled,
            double residualNegativePercent,
            double residualPositivePercent,
            int residualMinFloor,
            long gameTime
    ) {
        if (negativeAmount <= 0 && positiveAmount <= 0) {
            return false;
        }
        int newMining = Math.max(0, miningScore - Math.max(0, negativeAmount));
        int newCombat = Math.max(0, combatScore - Math.max(0, negativeAmount));
        int newDeath = Math.max(0, deathScore - Math.max(0, negativeAmount));
        int newFire = Math.max(0, fireScore - Math.max(0, negativeAmount));
        int newExploit = Math.max(0, exploitScore - Math.max(0, negativeAmount));
        int newFarm = Math.max(0, farmScore - Math.max(0, positiveAmount));
        int newBuild = Math.max(0, buildScore - Math.max(0, positiveAmount));
        int newTravel = Math.max(0, travelScore - Math.max(0, positiveAmount));
        if (residualEnabled) {
            newMining = Math.max(newMining, floorFor(maxMiningScore, residualNegativePercent, residualMinFloor));
            newCombat = Math.max(newCombat, floorFor(maxCombatScore, residualNegativePercent, residualMinFloor));
            newDeath = Math.max(newDeath, floorFor(maxDeathScore, residualNegativePercent, residualMinFloor));
            newFire = Math.max(newFire, floorFor(maxFireScore, residualNegativePercent, residualMinFloor));
            newExploit = Math.max(newExploit, floorFor(maxExploitScore, residualNegativePercent, residualMinFloor));
            newFarm = Math.max(newFarm, floorFor(maxFarmScore, residualPositivePercent, residualMinFloor));
            newBuild = Math.max(newBuild, floorFor(maxBuildScore, residualPositivePercent, residualMinFloor));
            newTravel = Math.max(newTravel, floorFor(maxTravelScore, residualPositivePercent, residualMinFloor));
        }
        return applyDecay(newMining, newCombat, newDeath, newFarm, newBuild, newFire, newTravel, newExploit, gameTime);
    }

    boolean decayPercent(
            double negativeFactor,
            double positiveFactor,
            boolean residualEnabled,
            double residualNegativePercent,
            double residualPositivePercent,
            int residualMinFloor,
            long gameTime
    ) {
        double negativeClamped = Math.max(0.0, Math.min(1.0, negativeFactor));
        double positiveClamped = Math.max(0.0, Math.min(1.0, positiveFactor));
        int newMining = (int) Math.floor(miningScore * negativeClamped);
        int newCombat = (int) Math.floor(combatScore * negativeClamped);
        int newDeath = (int) Math.floor(deathScore * negativeClamped);
        int newFire = (int) Math.floor(fireScore * negativeClamped);
        int newExploit = (int) Math.floor(exploitScore * negativeClamped);
        int newFarm = (int) Math.floor(farmScore * positiveClamped);
        int newBuild = (int) Math.floor(buildScore * positiveClamped);
        int newTravel = (int) Math.floor(travelScore * positiveClamped);
        if (residualEnabled) {
            newMining = Math.max(newMining, floorFor(maxMiningScore, residualNegativePercent, residualMinFloor));
            newCombat = Math.max(newCombat, floorFor(maxCombatScore, residualNegativePercent, residualMinFloor));
            newDeath = Math.max(newDeath, floorFor(maxDeathScore, residualNegativePercent, residualMinFloor));
            newFire = Math.max(newFire, floorFor(maxFireScore, residualNegativePercent, residualMinFloor));
            newExploit = Math.max(newExploit, floorFor(maxExploitScore, residualNegativePercent, residualMinFloor));
            newFarm = Math.max(newFarm, floorFor(maxFarmScore, residualPositivePercent, residualMinFloor));
            newBuild = Math.max(newBuild, floorFor(maxBuildScore, residualPositivePercent, residualMinFloor));
            newTravel = Math.max(newTravel, floorFor(maxTravelScore, residualPositivePercent, residualMinFloor));
        }
        return applyDecay(newMining, newCombat, newDeath, newFarm, newBuild, newFire, newTravel, newExploit, gameTime);
    }

    private boolean applyDecay(
            int newMining,
            int newCombat,
            int newDeath,
            int newFarm,
            int newBuild,
            int newFire,
            int newTravel,
            int newExploit,
            long gameTime
    ) {
        boolean changed = false;
        if (newMining != miningScore) {
            miningScore = newMining;
            changed = true;
        }
        if (newCombat != combatScore) {
            combatScore = newCombat;
            changed = true;
        }
        if (newDeath != deathScore) {
            deathScore = newDeath;
            changed = true;
        }
        if (newFarm != farmScore) {
            farmScore = newFarm;
            changed = true;
        }
        if (newBuild != buildScore) {
            buildScore = newBuild;
            changed = true;
        }
        if (newFire != fireScore) {
            fireScore = newFire;
            changed = true;
        }
        if (newTravel != travelScore) {
            travelScore = newTravel;
            changed = true;
        }
        if (newExploit != exploitScore) {
            exploitScore = newExploit;
            changed = true;
        }
        if (changed) {
            lastUpdated = gameTime;
        }
        return changed;
    }

    boolean isEmpty() {
        return miningScore == 0
                && combatScore == 0
                && deathScore == 0
                && farmScore == 0
                && buildScore == 0
                && fireScore == 0
                && travelScore == 0
                && exploitScore == 0;
    }

    boolean isHistoryEmpty() {
        return maxMiningScore == 0
                && maxCombatScore == 0
                && maxDeathScore == 0
                && maxFarmScore == 0
                && maxBuildScore == 0
                && maxFireScore == 0
                && maxTravelScore == 0
                && maxExploitScore == 0;
    }

    boolean normalize() {
        boolean changed = false;
        if (maxMiningScore < miningScore) {
            maxMiningScore = miningScore;
            changed = true;
        }
        if (maxCombatScore < combatScore) {
            maxCombatScore = combatScore;
            changed = true;
        }
        if (maxDeathScore < deathScore) {
            maxDeathScore = deathScore;
            changed = true;
        }
        if (maxFarmScore < farmScore) {
            maxFarmScore = farmScore;
            changed = true;
        }
        if (maxBuildScore < buildScore) {
            maxBuildScore = buildScore;
            changed = true;
        }
        if (maxFireScore < fireScore) {
            maxFireScore = fireScore;
            changed = true;
        }
        if (maxTravelScore < travelScore) {
            maxTravelScore = travelScore;
            changed = true;
        }
        if (maxExploitScore < exploitScore) {
            maxExploitScore = exploitScore;
            changed = true;
        }
        if (headlineUpdated <= 0L && !"neutral".equals(headlineId) && lastUpdated > 0L) {
            headlineUpdated = lastUpdated;
            changed = true;
        }
        return changed;
    }

    public int floorMining(boolean enabled, double percent, int minFloor) {
        return floorFor(maxMiningScore, percent, minFloor, enabled);
    }

    public int floorCombat(boolean enabled, double percent, int minFloor) {
        return floorFor(maxCombatScore, percent, minFloor, enabled);
    }

    public int floorDeath(boolean enabled, double percent, int minFloor) {
        return floorFor(maxDeathScore, percent, minFloor, enabled);
    }

    public int floorFarm(boolean enabled, double percent, int minFloor) {
        return floorFor(maxFarmScore, percent, minFloor, enabled);
    }

    public int floorBuild(boolean enabled, double percent, int minFloor) {
        return floorFor(maxBuildScore, percent, minFloor, enabled);
    }

    public int floorFire(boolean enabled, double percent, int minFloor) {
        return floorFor(maxFireScore, percent, minFloor, enabled);
    }

    public int floorTravel(boolean enabled, double percent, int minFloor) {
        return floorFor(maxTravelScore, percent, minFloor, enabled);
    }

    public int floorExploit(boolean enabled, double percent, int minFloor) {
        return floorFor(maxExploitScore, percent, minFloor, enabled);
    }

    private static int floorFor(int maxScore, double percent, int minFloor) {
        return floorFor(maxScore, percent, minFloor, true);
    }

    private static int floorFor(int maxScore, double percent, int minFloor, boolean enabled) {
        if (!enabled) {
            return 0;
        }
        double clamped = Math.max(0.0, Math.min(1.0, percent));
        int floor = (int) Math.floor(maxScore * clamped);
        return Math.max(Math.max(0, minFloor), Math.max(0, floor));
    }

    private static int maxFromList(List<Integer> values, int index, int fallback) {
        if (values == null || index < 0 || index >= values.size()) {
            return Math.max(0, fallback);
        }
        return Math.max(0, values.get(index));
    }
}
