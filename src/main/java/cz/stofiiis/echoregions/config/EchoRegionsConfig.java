package cz.stofiiis.echoregions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EchoRegionsConfig {
    public enum DecayMode {
        FLAT,
        PERCENT
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue THRESHOLD_MINING;
    public static final ModConfigSpec.IntValue THRESHOLD_COMBAT;
    public static final ModConfigSpec.IntValue THRESHOLD_DEATH;
    public static final ModConfigSpec.IntValue THRESHOLD_FARM;
    public static final ModConfigSpec.IntValue THRESHOLD_BUILD;
    public static final ModConfigSpec.IntValue THRESHOLD_FIRE;
    public static final ModConfigSpec.IntValue THRESHOLD_TRAVEL;
    public static final ModConfigSpec.IntValue THRESHOLD_EXPLOIT;

    public static final ModConfigSpec.IntValue DECAY_INTERVAL_MINUTES;
    public static final ModConfigSpec.EnumValue<DecayMode> DECAY_MODE;
    public static final ModConfigSpec.IntValue NEGATIVE_DECAY_FLAT_AMOUNT;
    public static final ModConfigSpec.DoubleValue NEGATIVE_DECAY_PERCENT;
    public static final ModConfigSpec.IntValue POSITIVE_DECAY_FLAT_AMOUNT;
    public static final ModConfigSpec.DoubleValue POSITIVE_DECAY_PERCENT;
    public static final ModConfigSpec.DoubleValue HEADLINE_KEEP_FACTOR;
    public static final ModConfigSpec.DoubleValue HEADLINE_SWITCH_RATIO;
    public static final ModConfigSpec.IntValue MIN_HEADLINE_DURATION_MINUTES;

    public static final ModConfigSpec.BooleanValue RESIDUAL_ENABLED;
    public static final ModConfigSpec.DoubleValue RESIDUAL_NEGATIVE_PERCENT;
    public static final ModConfigSpec.DoubleValue RESIDUAL_POSITIVE_PERCENT;
    public static final ModConfigSpec.IntValue RESIDUAL_MIN_FLOOR;
    public static final ModConfigSpec.BooleanValue RESIDUAL_AFFECTS_HEADLINE;

    public static final ModConfigSpec.DoubleValue SCARRED_PEBBLE_CHANCE;
    public static final ModConfigSpec.DoubleValue HAUNTED_ECTOPLASM_CHANCE;
    public static final ModConfigSpec.DoubleValue WAR_TORN_STRENGTH_CHANCE;

    public static final ModConfigSpec.BooleanValue AMBIENT_ENABLED;
    public static final ModConfigSpec.IntValue AMBIENT_CHECK_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue AMBIENT_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue AMBIENT_ENTRY_ENABLED;
    public static final ModConfigSpec.DoubleValue AMBIENT_ENTRY_CHANCE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AMBIENT_ENTRY_PARTICLE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AMBIENT_ENTRY_SOUND_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue SCARRED_AMBIENT_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue SCARRED_AMBIENT_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue SCARRED_AMBIENT_CHANCE_HIGH;
    public static final ModConfigSpec.IntValue SCARRED_AMBIENT_PARTICLES_LOW;
    public static final ModConfigSpec.IntValue SCARRED_AMBIENT_PARTICLES_MED;
    public static final ModConfigSpec.IntValue SCARRED_AMBIENT_PARTICLES_HIGH;
    public static final ModConfigSpec.DoubleValue SCARRED_SOUND_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue SCARRED_SOUND_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue SCARRED_SOUND_CHANCE_HIGH;
    public static final ModConfigSpec.DoubleValue SCARRED_SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue SCARRED_SOUND_PITCH;
    public static final ModConfigSpec.DoubleValue SCARRED_MINING_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue SCARRED_MINING_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue SCARRED_MINING_CHANCE_HIGH;
    public static final ModConfigSpec.IntValue SCARRED_MINING_PARTICLES_LOW;
    public static final ModConfigSpec.IntValue SCARRED_MINING_PARTICLES_MED;
    public static final ModConfigSpec.IntValue SCARRED_MINING_PARTICLES_HIGH;

    public static final ModConfigSpec.DoubleValue HAUNTED_AMBIENT_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue HAUNTED_AMBIENT_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue HAUNTED_AMBIENT_CHANCE_HIGH;
    public static final ModConfigSpec.IntValue HAUNTED_AMBIENT_PARTICLES_LOW;
    public static final ModConfigSpec.IntValue HAUNTED_AMBIENT_PARTICLES_MED;
    public static final ModConfigSpec.IntValue HAUNTED_AMBIENT_PARTICLES_HIGH;
    public static final ModConfigSpec.DoubleValue HAUNTED_SOUND_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue HAUNTED_SOUND_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue HAUNTED_SOUND_CHANCE_HIGH;
    public static final ModConfigSpec.DoubleValue HAUNTED_SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue HAUNTED_SOUND_PITCH;

    public static final ModConfigSpec.DoubleValue WAR_TORN_AMBIENT_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue WAR_TORN_AMBIENT_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue WAR_TORN_AMBIENT_CHANCE_HIGH;
    public static final ModConfigSpec.IntValue WAR_TORN_AMBIENT_PARTICLES_LOW;
    public static final ModConfigSpec.IntValue WAR_TORN_AMBIENT_PARTICLES_MED;
    public static final ModConfigSpec.IntValue WAR_TORN_AMBIENT_PARTICLES_HIGH;
    public static final ModConfigSpec.DoubleValue WAR_TORN_SOUND_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue WAR_TORN_SOUND_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue WAR_TORN_SOUND_CHANCE_HIGH;
    public static final ModConfigSpec.DoubleValue WAR_TORN_SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue WAR_TORN_SOUND_PITCH;
    public static final ModConfigSpec.DoubleValue WAR_TORN_COMBAT_CHANCE_LOW;
    public static final ModConfigSpec.DoubleValue WAR_TORN_COMBAT_CHANCE_MED;
    public static final ModConfigSpec.DoubleValue WAR_TORN_COMBAT_CHANCE_HIGH;
    public static final ModConfigSpec.IntValue WAR_TORN_COMBAT_PARTICLES_LOW;
    public static final ModConfigSpec.IntValue WAR_TORN_COMBAT_PARTICLES_MED;
    public static final ModConfigSpec.IntValue WAR_TORN_COMBAT_PARTICLES_HIGH;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("thresholds");
        THRESHOLD_MINING = BUILDER
                .comment("Score required for SCARRED.")
                .defineInRange("thresholdMining", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_COMBAT = BUILDER
                .comment("Score required for WAR_TORN.")
                .defineInRange("thresholdCombat", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_DEATH = BUILDER
                .comment("Score required for HAUNTED.")
                .defineInRange("thresholdDeath", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_FARM = BUILDER
                .comment("Score required for CULTIVATED.")
                .defineInRange("thresholdFarm", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_BUILD = BUILDER
                .comment("Score required for SETTLED.")
                .defineInRange("thresholdBuild", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_FIRE = BUILDER
                .comment("Score required for BLIGHTED.")
                .defineInRange("thresholdFire", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_TRAVEL = BUILDER
                .comment("Score required for TRAVELLED.")
                .defineInRange("thresholdTravel", 10, 0, Integer.MAX_VALUE);
        THRESHOLD_EXPLOIT = BUILDER
                .comment("Score required for EXPLOITED.")
                .defineInRange("thresholdExploit", 10, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("decay");
        DECAY_INTERVAL_MINUTES = BUILDER
                .comment("How often to decay scores, in minutes.")
                .defineInRange("intervalMinutes", 5, 1, 10080);
        DECAY_MODE = BUILDER
                .comment("Decay mode: FLAT subtracts a fixed amount; PERCENT multiplies by a factor.")
                .defineEnum("mode", DecayMode.FLAT);
        NEGATIVE_DECAY_FLAT_AMOUNT = BUILDER
                .comment("Amount subtracted each decay for negative scores (mining/combat/death/fire/exploit).")
                .defineInRange("negativeFlatAmount", 1, 0, Integer.MAX_VALUE);
        NEGATIVE_DECAY_PERCENT = BUILDER
                .comment("Multiplier applied each decay for negative scores (0.0 - 1.0).")
                .defineInRange("negativePercent", 0.995, 0.0, 1.0);
        POSITIVE_DECAY_FLAT_AMOUNT = BUILDER
                .comment("Amount subtracted each decay for positive scores (farm/build/travel).")
                .defineInRange("positiveFlatAmount", 2, 0, Integer.MAX_VALUE);
        POSITIVE_DECAY_PERCENT = BUILDER
                .comment("Multiplier applied each decay for positive scores (0.0 - 1.0).")
                .defineInRange("positivePercent", 0.98, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("headline");
        HEADLINE_KEEP_FACTOR = BUILDER
                .comment("Keep headline as long as its score stays above threshold * factor.")
                .defineInRange("keepThresholdFactor", 0.8, 0.0, 2.0);
        HEADLINE_SWITCH_RATIO = BUILDER
                .comment("Switch headline if another score reaches currentScore * ratio.")
                .defineInRange("switchRatio", 1.25, 1.0, 10.0);
        MIN_HEADLINE_DURATION_MINUTES = BUILDER
                .comment("Minimum time to keep a headline before it may switch.")
                .defineInRange("minDurationMinutes", 5, 0, 1440);
        BUILDER.pop();

        BUILDER.push("residual");
        RESIDUAL_ENABLED = BUILDER
                .comment("Enable residual floors based on historical maxima.")
                .define("enabled", true);
        RESIDUAL_NEGATIVE_PERCENT = BUILDER
                .comment("Residual floor percent for negative scores (mining/combat/death/fire/exploit).")
                .defineInRange("negativePercent", 0.10, 0.0, 1.0);
        RESIDUAL_POSITIVE_PERCENT = BUILDER
                .comment("Residual floor percent for positive scores (farm/build/travel).")
                .defineInRange("positivePercent", 0.0, 0.0, 1.0);
        RESIDUAL_MIN_FLOOR = BUILDER
                .comment("Minimum residual floor applied to all scores.")
                .defineInRange("minFloor", 0, 0, Integer.MAX_VALUE);
        RESIDUAL_AFFECTS_HEADLINE = BUILDER
                .comment("Whether residual floors influence headline selection.")
                .define("affectsHeadline", true);
        BUILDER.pop();

        BUILDER.push("drops");
        SCARRED_PEBBLE_CHANCE = BUILDER
                .comment("Chance for SCARRED extra drop (future use).")
                .defineInRange("scarredPebbleChance", 0.15, 0.0, 1.0);
        HAUNTED_ECTOPLASM_CHANCE = BUILDER
                .comment("Chance for HAUNTED ectoplasm drop (future use).")
                .defineInRange("hauntedEctoplasmChance", 0.15, 0.0, 1.0);
        WAR_TORN_STRENGTH_CHANCE = BUILDER
                .comment("Chance for WAR_TORN strength effect (future use).")
                .defineInRange("warTornStrengthChance", 0.05, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("ambient");
        AMBIENT_ENABLED = BUILDER
                .comment("Enable ambient particles/sounds based on region headline.")
                .define("enabled", true);
        AMBIENT_CHECK_INTERVAL_TICKS = BUILDER
                .comment("How often to evaluate ambient cues (ticks).")
                .defineInRange("checkIntervalTicks", 40, 1, 1200);
        AMBIENT_COOLDOWN_TICKS = BUILDER
                .comment("Minimum ticks between ambient cues per player/state.")
                .defineInRange("cooldownTicks", 100, 0, 12000);
        AMBIENT_ENTRY_ENABLED = BUILDER
                .comment("Trigger an extra ambient cue when entering a new region.")
                .define("entryEnabled", true);
        AMBIENT_ENTRY_CHANCE_MULTIPLIER = BUILDER
                .comment("Multiplier applied to ambient chance on region entry.")
                .defineInRange("entryChanceMultiplier", 1.8, 0.0, 10.0);
        AMBIENT_ENTRY_PARTICLE_MULTIPLIER = BUILDER
                .comment("Multiplier applied to particle count on region entry.")
                .defineInRange("entryParticleMultiplier", 1.5, 0.0, 10.0);
        AMBIENT_ENTRY_SOUND_MULTIPLIER = BUILDER
                .comment("Multiplier applied to haunted sound chance on region entry.")
                .defineInRange("entrySoundMultiplier", 1.5, 0.0, 10.0);

        BUILDER.push("scarred");
        SCARRED_AMBIENT_CHANCE_LOW = BUILDER.defineInRange("ambientChanceLow", 0.25, 0.0, 1.0);
        SCARRED_AMBIENT_CHANCE_MED = BUILDER.defineInRange("ambientChanceMed", 0.4, 0.0, 1.0);
        SCARRED_AMBIENT_CHANCE_HIGH = BUILDER.defineInRange("ambientChanceHigh", 0.6, 0.0, 1.0);
        SCARRED_AMBIENT_PARTICLES_LOW = BUILDER.defineInRange("ambientParticlesLow", 6, 0, 200);
        SCARRED_AMBIENT_PARTICLES_MED = BUILDER.defineInRange("ambientParticlesMed", 10, 0, 200);
        SCARRED_AMBIENT_PARTICLES_HIGH = BUILDER.defineInRange("ambientParticlesHigh", 14, 0, 200);
        SCARRED_SOUND_CHANCE_LOW = BUILDER.defineInRange("soundChanceLow", 0.25, 0.0, 1.0);
        SCARRED_SOUND_CHANCE_MED = BUILDER.defineInRange("soundChanceMed", 0.4, 0.0, 1.0);
        SCARRED_SOUND_CHANCE_HIGH = BUILDER.defineInRange("soundChanceHigh", 0.6, 0.0, 1.0);
        SCARRED_SOUND_VOLUME = BUILDER.defineInRange("soundVolume", 0.5, 0.0, 2.0);
        SCARRED_SOUND_PITCH = BUILDER.defineInRange("soundPitch", 0.9, 0.5, 2.0);
        SCARRED_MINING_CHANCE_LOW = BUILDER.defineInRange("miningChanceLow", 0.4, 0.0, 1.0);
        SCARRED_MINING_CHANCE_MED = BUILDER.defineInRange("miningChanceMed", 0.6, 0.0, 1.0);
        SCARRED_MINING_CHANCE_HIGH = BUILDER.defineInRange("miningChanceHigh", 0.8, 0.0, 1.0);
        SCARRED_MINING_PARTICLES_LOW = BUILDER.defineInRange("miningParticlesLow", 6, 0, 200);
        SCARRED_MINING_PARTICLES_MED = BUILDER.defineInRange("miningParticlesMed", 10, 0, 200);
        SCARRED_MINING_PARTICLES_HIGH = BUILDER.defineInRange("miningParticlesHigh", 14, 0, 200);
        BUILDER.pop();

        BUILDER.push("haunted");
        HAUNTED_AMBIENT_CHANCE_LOW = BUILDER.defineInRange("ambientChanceLow", 0.2, 0.0, 1.0);
        HAUNTED_AMBIENT_CHANCE_MED = BUILDER.defineInRange("ambientChanceMed", 0.35, 0.0, 1.0);
        HAUNTED_AMBIENT_CHANCE_HIGH = BUILDER.defineInRange("ambientChanceHigh", 0.5, 0.0, 1.0);
        HAUNTED_AMBIENT_PARTICLES_LOW = BUILDER.defineInRange("ambientParticlesLow", 5, 0, 200);
        HAUNTED_AMBIENT_PARTICLES_MED = BUILDER.defineInRange("ambientParticlesMed", 9, 0, 200);
        HAUNTED_AMBIENT_PARTICLES_HIGH = BUILDER.defineInRange("ambientParticlesHigh", 12, 0, 200);
        HAUNTED_SOUND_CHANCE_LOW = BUILDER.defineInRange("soundChanceLow", 0.2, 0.0, 1.0);
        HAUNTED_SOUND_CHANCE_MED = BUILDER.defineInRange("soundChanceMed", 0.35, 0.0, 1.0);
        HAUNTED_SOUND_CHANCE_HIGH = BUILDER.defineInRange("soundChanceHigh", 0.5, 0.0, 1.0);
        HAUNTED_SOUND_VOLUME = BUILDER.defineInRange("soundVolume", 0.4, 0.0, 2.0);
        HAUNTED_SOUND_PITCH = BUILDER.defineInRange("soundPitch", 0.8, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.push("warTorn");
        WAR_TORN_AMBIENT_CHANCE_LOW = BUILDER.defineInRange("ambientChanceLow", 0.2, 0.0, 1.0);
        WAR_TORN_AMBIENT_CHANCE_MED = BUILDER.defineInRange("ambientChanceMed", 0.35, 0.0, 1.0);
        WAR_TORN_AMBIENT_CHANCE_HIGH = BUILDER.defineInRange("ambientChanceHigh", 0.5, 0.0, 1.0);
        WAR_TORN_AMBIENT_PARTICLES_LOW = BUILDER.defineInRange("ambientParticlesLow", 6, 0, 200);
        WAR_TORN_AMBIENT_PARTICLES_MED = BUILDER.defineInRange("ambientParticlesMed", 10, 0, 200);
        WAR_TORN_AMBIENT_PARTICLES_HIGH = BUILDER.defineInRange("ambientParticlesHigh", 14, 0, 200);
        WAR_TORN_SOUND_CHANCE_LOW = BUILDER.defineInRange("soundChanceLow", 0.2, 0.0, 1.0);
        WAR_TORN_SOUND_CHANCE_MED = BUILDER.defineInRange("soundChanceMed", 0.35, 0.0, 1.0);
        WAR_TORN_SOUND_CHANCE_HIGH = BUILDER.defineInRange("soundChanceHigh", 0.5, 0.0, 1.0);
        WAR_TORN_SOUND_VOLUME = BUILDER.defineInRange("soundVolume", 0.55, 0.0, 2.0);
        WAR_TORN_SOUND_PITCH = BUILDER.defineInRange("soundPitch", 0.8, 0.5, 2.0);
        WAR_TORN_COMBAT_CHANCE_LOW = BUILDER.defineInRange("combatChanceLow", 0.35, 0.0, 1.0);
        WAR_TORN_COMBAT_CHANCE_MED = BUILDER.defineInRange("combatChanceMed", 0.55, 0.0, 1.0);
        WAR_TORN_COMBAT_CHANCE_HIGH = BUILDER.defineInRange("combatChanceHigh", 0.75, 0.0, 1.0);
        WAR_TORN_COMBAT_PARTICLES_LOW = BUILDER.defineInRange("combatParticlesLow", 8, 0, 200);
        WAR_TORN_COMBAT_PARTICLES_MED = BUILDER.defineInRange("combatParticlesMed", 12, 0, 200);
        WAR_TORN_COMBAT_PARTICLES_HIGH = BUILDER.defineInRange("combatParticlesHigh", 16, 0, 200);
        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private EchoRegionsConfig() {
    }
}
