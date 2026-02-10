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

    public static final ModConfigSpec.IntValue DECAY_INTERVAL_MINUTES;
    public static final ModConfigSpec.EnumValue<DecayMode> DECAY_MODE;
    public static final ModConfigSpec.IntValue DECAY_FLAT_AMOUNT;
    public static final ModConfigSpec.DoubleValue DECAY_PERCENT;

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
        BUILDER.pop();

        BUILDER.push("decay");
        DECAY_INTERVAL_MINUTES = BUILDER
                .comment("How often to decay scores, in minutes.")
                .defineInRange("intervalMinutes", 5, 1, 10080);
        DECAY_MODE = BUILDER
                .comment("Decay mode: FLAT subtracts a fixed amount; PERCENT multiplies by a factor.")
                .defineEnum("mode", DecayMode.FLAT);
        DECAY_FLAT_AMOUNT = BUILDER
                .comment("Amount subtracted each decay when using FLAT mode.")
                .defineInRange("flatAmount", 1, 0, Integer.MAX_VALUE);
        DECAY_PERCENT = BUILDER
                .comment("Multiplier applied each decay when using PERCENT mode (0.0 - 1.0).")
                .defineInRange("percent", 0.99, 0.0, 1.0);
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
