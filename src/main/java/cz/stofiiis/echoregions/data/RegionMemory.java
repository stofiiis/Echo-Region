package cz.stofiiis.echoregions.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class RegionMemory {
    public static final Codec<RegionMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("miningScore", 0).forGetter(RegionMemory::getMiningScore),
            Codec.INT.optionalFieldOf("combatScore", 0).forGetter(RegionMemory::getCombatScore),
            Codec.INT.optionalFieldOf("deathScore", 0).forGetter(RegionMemory::getDeathScore),
            Codec.INT.optionalFieldOf("farmScore", 0).forGetter(RegionMemory::getFarmScore),
            Codec.LONG.optionalFieldOf("lastUpdated", 0L).forGetter(RegionMemory::getLastUpdated)
    ).apply(instance, RegionMemory::new));

    private int miningScore;
    private int combatScore;
    private int deathScore;
    private int farmScore;
    private long lastUpdated;

    public RegionMemory() {
        this(0, 0, 0, 0, 0L);
    }

    public RegionMemory(int miningScore, int combatScore, int deathScore, int farmScore, long lastUpdated) {
        this.miningScore = Math.max(0, miningScore);
        this.combatScore = Math.max(0, combatScore);
        this.deathScore = Math.max(0, deathScore);
        this.farmScore = Math.max(0, farmScore);
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

    boolean decay(int amount, long gameTime) {
        if (amount <= 0) {
            return false;
        }
        boolean changed = false;
        int newMining = Math.max(0, miningScore - amount);
        if (newMining != miningScore) {
            miningScore = newMining;
            changed = true;
        }
        int newCombat = Math.max(0, combatScore - amount);
        if (newCombat != combatScore) {
            combatScore = newCombat;
            changed = true;
        }
        int newDeath = Math.max(0, deathScore - amount);
        if (newDeath != deathScore) {
            deathScore = newDeath;
            changed = true;
        }
        int newFarm = Math.max(0, farmScore - amount);
        if (newFarm != farmScore) {
            farmScore = newFarm;
            changed = true;
        }
        if (changed) {
            lastUpdated = gameTime;
        }
        return changed;
    }

    boolean isEmpty() {
        return miningScore == 0 && combatScore == 0 && deathScore == 0 && farmScore == 0;
    }
}
