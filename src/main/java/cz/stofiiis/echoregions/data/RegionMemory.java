package cz.stofiiis.echoregions.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class RegionMemory {
    public static final Codec<RegionMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("miningScore", 0).forGetter(RegionMemory::getMiningScore),
            Codec.INT.optionalFieldOf("combatScore", 0).forGetter(RegionMemory::getCombatScore),
            Codec.INT.optionalFieldOf("deathScore", 0).forGetter(RegionMemory::getDeathScore),
            Codec.INT.optionalFieldOf("farmScore", 0).forGetter(RegionMemory::getFarmScore),
            Codec.INT.optionalFieldOf("buildScore", 0).forGetter(RegionMemory::getBuildScore),
            Codec.INT.optionalFieldOf("fireScore", 0).forGetter(RegionMemory::getFireScore),
            Codec.INT.optionalFieldOf("travelScore", 0).forGetter(RegionMemory::getTravelScore),
            Codec.INT.optionalFieldOf("exploitScore", 0).forGetter(RegionMemory::getExploitScore),
            Codec.LONG.optionalFieldOf("lastUpdated", 0L).forGetter(RegionMemory::getLastUpdated)
    ).apply(instance, RegionMemory::new));

    private int miningScore;
    private int combatScore;
    private int deathScore;
    private int farmScore;
    private int buildScore;
    private int fireScore;
    private int travelScore;
    private int exploitScore;
    private long lastUpdated;

    public RegionMemory() {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0L);
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
        this.miningScore = Math.max(0, miningScore);
        this.combatScore = Math.max(0, combatScore);
        this.deathScore = Math.max(0, deathScore);
        this.farmScore = Math.max(0, farmScore);
        this.buildScore = Math.max(0, buildScore);
        this.fireScore = Math.max(0, fireScore);
        this.travelScore = Math.max(0, travelScore);
        this.exploitScore = Math.max(0, exploitScore);
        this.lastUpdated = Math.max(0L, lastUpdated);
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

    public long getLastUpdated() {
        return lastUpdated;
    }

    void addMining(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        miningScore += amount;
        lastUpdated = gameTime;
    }

    void addCombat(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        combatScore += amount;
        lastUpdated = gameTime;
    }

    void addDeath(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        deathScore += amount;
        lastUpdated = gameTime;
    }

    void addFarm(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        farmScore += amount;
        lastUpdated = gameTime;
    }

    void addBuild(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        buildScore += amount;
        lastUpdated = gameTime;
    }

    void addFire(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        fireScore += amount;
        lastUpdated = gameTime;
    }

    void addTravel(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        travelScore += amount;
        lastUpdated = gameTime;
    }

    void addExploit(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        exploitScore += amount;
        lastUpdated = gameTime;
    }

    boolean decayFlat(int amount, long gameTime) {
        if (amount <= 0) {
            return false;
        }
        int newMining = Math.max(0, miningScore - amount);
        int newCombat = Math.max(0, combatScore - amount);
        int newDeath = Math.max(0, deathScore - amount);
        int newFarm = Math.max(0, farmScore - amount);
        int newBuild = Math.max(0, buildScore - amount);
        int newFire = Math.max(0, fireScore - amount);
        int newTravel = Math.max(0, travelScore - amount);
        int newExploit = Math.max(0, exploitScore - amount);
        return applyDecay(newMining, newCombat, newDeath, newFarm, newBuild, newFire, newTravel, newExploit, gameTime);
    }

    boolean decayPercent(double factor, long gameTime) {
        double clamped = Math.max(0.0, Math.min(1.0, factor));
        int newMining = (int) Math.floor(miningScore * clamped);
        int newCombat = (int) Math.floor(combatScore * clamped);
        int newDeath = (int) Math.floor(deathScore * clamped);
        int newFarm = (int) Math.floor(farmScore * clamped);
        int newBuild = (int) Math.floor(buildScore * clamped);
        int newFire = (int) Math.floor(fireScore * clamped);
        int newTravel = (int) Math.floor(travelScore * clamped);
        int newExploit = (int) Math.floor(exploitScore * clamped);
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
}
