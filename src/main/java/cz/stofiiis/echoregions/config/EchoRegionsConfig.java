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

        SPEC = BUILDER.build();
    }

    private EchoRegionsConfig() {
    }
}
