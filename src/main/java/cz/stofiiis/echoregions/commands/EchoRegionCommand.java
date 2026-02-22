package cz.stofiiis.echoregions.commands;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import cz.stofiiis.echoregions.EchoRegions;
import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import cz.stofiiis.echoregions.debug.DebugOverlaySync;
import cz.stofiiis.echoregions.debug.DebugOverlayTracker;
import cz.stofiiis.echoregions.data.RegionMemory;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import cz.stofiiis.echoregions.data.RegionPos;
import cz.stofiiis.echoregions.region.RegionState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EchoRegionCommand {
    private EchoRegionCommand() {
    }

    private static boolean debugEnabled = true;
    private static final int MAX_MAP_RADIUS = 8;

    private enum ConfigType {
        INT,
        DOUBLE,
        ENUM,
        BOOLEAN
    }

    private record ConfigEntry(String key, ModConfigSpec.ConfigValue<?> value, ConfigType type) {
    }

    private static final Map<String, ConfigEntry> CONFIG_KEYS = createConfigKeys();

    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEY_SUGGESTIONS = (context, builder) -> {
        for (ConfigEntry entry : CONFIG_KEYS.values()) {
            builder.suggest(entry.key());
        }
        return builder.buildFuture();
    };

    private static Map<String, ConfigEntry> createConfigKeys() {
        Map<String, ConfigEntry> entries = new LinkedHashMap<>();
        register(entries, "thresholdMining", EchoRegionsConfig.THRESHOLD_MINING, ConfigType.INT);
        register(entries, "thresholdCombat", EchoRegionsConfig.THRESHOLD_COMBAT, ConfigType.INT);
        register(entries, "thresholdDeath", EchoRegionsConfig.THRESHOLD_DEATH, ConfigType.INT);
        register(entries, "thresholdFarm", EchoRegionsConfig.THRESHOLD_FARM, ConfigType.INT);
        register(entries, "thresholdBuild", EchoRegionsConfig.THRESHOLD_BUILD, ConfigType.INT);
        register(entries, "thresholdFire", EchoRegionsConfig.THRESHOLD_FIRE, ConfigType.INT);
        register(entries, "thresholdTravel", EchoRegionsConfig.THRESHOLD_TRAVEL, ConfigType.INT);
        register(entries, "thresholdExploit", EchoRegionsConfig.THRESHOLD_EXPLOIT, ConfigType.INT);
        register(entries, "decayIntervalMinutes", EchoRegionsConfig.DECAY_INTERVAL_MINUTES, ConfigType.INT);
        register(entries, "decayMode", EchoRegionsConfig.DECAY_MODE, ConfigType.ENUM);
        register(entries, "negativeDecayFlatAmount", EchoRegionsConfig.NEGATIVE_DECAY_FLAT_AMOUNT, ConfigType.INT);
        register(entries, "negativeDecayPercent", EchoRegionsConfig.NEGATIVE_DECAY_PERCENT, ConfigType.DOUBLE);
        register(entries, "positiveDecayFlatAmount", EchoRegionsConfig.POSITIVE_DECAY_FLAT_AMOUNT, ConfigType.INT);
        register(entries, "positiveDecayPercent", EchoRegionsConfig.POSITIVE_DECAY_PERCENT, ConfigType.DOUBLE);
        register(entries, "headlineKeepThresholdFactor", EchoRegionsConfig.HEADLINE_KEEP_FACTOR, ConfigType.DOUBLE);
        register(entries, "headlineSwitchRatio", EchoRegionsConfig.HEADLINE_SWITCH_RATIO, ConfigType.DOUBLE);
        register(entries, "headlineMinDurationMinutes", EchoRegionsConfig.MIN_HEADLINE_DURATION_MINUTES, ConfigType.INT);
        register(entries, "residualEnabled", EchoRegionsConfig.RESIDUAL_ENABLED, ConfigType.BOOLEAN);
        register(entries, "residualNegativePercent", EchoRegionsConfig.RESIDUAL_NEGATIVE_PERCENT, ConfigType.DOUBLE);
        register(entries, "residualPositivePercent", EchoRegionsConfig.RESIDUAL_POSITIVE_PERCENT, ConfigType.DOUBLE);
        register(entries, "residualMinFloor", EchoRegionsConfig.RESIDUAL_MIN_FLOOR, ConfigType.INT);
        register(entries, "residualAffectsHeadline", EchoRegionsConfig.RESIDUAL_AFFECTS_HEADLINE, ConfigType.BOOLEAN);
        register(entries, "scarredPebbleChance", EchoRegionsConfig.SCARRED_PEBBLE_CHANCE, ConfigType.DOUBLE);
        register(entries, "hauntedEctoplasmChance", EchoRegionsConfig.HAUNTED_ECTOPLASM_CHANCE, ConfigType.DOUBLE);
        register(entries, "warTornStrengthChance", EchoRegionsConfig.WAR_TORN_STRENGTH_CHANCE, ConfigType.DOUBLE);
        register(entries, "ambientEnabled", EchoRegionsConfig.AMBIENT_ENABLED, ConfigType.BOOLEAN);
        register(entries, "ambientCheckIntervalTicks", EchoRegionsConfig.AMBIENT_CHECK_INTERVAL_TICKS, ConfigType.INT);
        register(entries, "ambientCooldownTicks", EchoRegionsConfig.AMBIENT_COOLDOWN_TICKS, ConfigType.INT);
        register(entries, "ambientSoundCooldownTicks", EchoRegionsConfig.AMBIENT_SOUND_COOLDOWN_TICKS, ConfigType.INT);
        register(entries, "ambientParticleBurstCap", EchoRegionsConfig.AMBIENT_PARTICLE_BURST_CAP, ConfigType.INT);
        register(entries, "ambientEntryEnabled", EchoRegionsConfig.AMBIENT_ENTRY_ENABLED, ConfigType.BOOLEAN);
        register(entries, "ambientEntryChanceMultiplier", EchoRegionsConfig.AMBIENT_ENTRY_CHANCE_MULTIPLIER, ConfigType.DOUBLE);
        register(entries, "ambientEntryParticleMultiplier", EchoRegionsConfig.AMBIENT_ENTRY_PARTICLE_MULTIPLIER, ConfigType.DOUBLE);
        register(entries, "ambientEntrySoundMultiplier", EchoRegionsConfig.AMBIENT_ENTRY_SOUND_MULTIPLIER, ConfigType.DOUBLE);
        register(entries, "regionEntryFeedbackEnabled", EchoRegionsConfig.REGION_ENTRY_FEEDBACK_ENABLED, ConfigType.BOOLEAN);
        register(entries, "regionEntryAuraDurationTicks", EchoRegionsConfig.REGION_ENTRY_AURA_DURATION_TICKS, ConfigType.INT);
        register(entries, "scarredAmbientChanceLow", EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "scarredAmbientChanceMed", EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "scarredAmbientChanceHigh", EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "scarredAmbientParticlesLow", EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_LOW, ConfigType.INT);
        register(entries, "scarredAmbientParticlesMed", EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_MED, ConfigType.INT);
        register(entries, "scarredAmbientParticlesHigh", EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_HIGH, ConfigType.INT);
        register(entries, "scarredSoundChanceLow", EchoRegionsConfig.SCARRED_SOUND_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "scarredSoundChanceMed", EchoRegionsConfig.SCARRED_SOUND_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "scarredSoundChanceHigh", EchoRegionsConfig.SCARRED_SOUND_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "scarredSoundVolume", EchoRegionsConfig.SCARRED_SOUND_VOLUME, ConfigType.DOUBLE);
        register(entries, "scarredSoundPitch", EchoRegionsConfig.SCARRED_SOUND_PITCH, ConfigType.DOUBLE);
        register(entries, "scarredMiningChanceLow", EchoRegionsConfig.SCARRED_MINING_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "scarredMiningChanceMed", EchoRegionsConfig.SCARRED_MINING_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "scarredMiningChanceHigh", EchoRegionsConfig.SCARRED_MINING_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "scarredMiningParticlesLow", EchoRegionsConfig.SCARRED_MINING_PARTICLES_LOW, ConfigType.INT);
        register(entries, "scarredMiningParticlesMed", EchoRegionsConfig.SCARRED_MINING_PARTICLES_MED, ConfigType.INT);
        register(entries, "scarredMiningParticlesHigh", EchoRegionsConfig.SCARRED_MINING_PARTICLES_HIGH, ConfigType.INT);
        register(entries, "hauntedAmbientChanceLow", EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "hauntedAmbientChanceMed", EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "hauntedAmbientChanceHigh", EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "hauntedAmbientParticlesLow", EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_LOW, ConfigType.INT);
        register(entries, "hauntedAmbientParticlesMed", EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_MED, ConfigType.INT);
        register(entries, "hauntedAmbientParticlesHigh", EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_HIGH, ConfigType.INT);
        register(entries, "hauntedSoundChanceLow", EchoRegionsConfig.HAUNTED_SOUND_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "hauntedSoundChanceMed", EchoRegionsConfig.HAUNTED_SOUND_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "hauntedSoundChanceHigh", EchoRegionsConfig.HAUNTED_SOUND_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "hauntedSoundVolume", EchoRegionsConfig.HAUNTED_SOUND_VOLUME, ConfigType.DOUBLE);
        register(entries, "hauntedSoundPitch", EchoRegionsConfig.HAUNTED_SOUND_PITCH, ConfigType.DOUBLE);
        register(entries, "warTornAmbientChanceLow", EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "warTornAmbientChanceMed", EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "warTornAmbientChanceHigh", EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "warTornAmbientParticlesLow", EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_LOW, ConfigType.INT);
        register(entries, "warTornAmbientParticlesMed", EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_MED, ConfigType.INT);
        register(entries, "warTornAmbientParticlesHigh", EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_HIGH, ConfigType.INT);
        register(entries, "warTornSoundChanceLow", EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "warTornSoundChanceMed", EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "warTornSoundChanceHigh", EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "warTornSoundVolume", EchoRegionsConfig.WAR_TORN_SOUND_VOLUME, ConfigType.DOUBLE);
        register(entries, "warTornSoundPitch", EchoRegionsConfig.WAR_TORN_SOUND_PITCH, ConfigType.DOUBLE);
        register(entries, "warTornCombatChanceLow", EchoRegionsConfig.WAR_TORN_COMBAT_CHANCE_LOW, ConfigType.DOUBLE);
        register(entries, "warTornCombatChanceMed", EchoRegionsConfig.WAR_TORN_COMBAT_CHANCE_MED, ConfigType.DOUBLE);
        register(entries, "warTornCombatChanceHigh", EchoRegionsConfig.WAR_TORN_COMBAT_CHANCE_HIGH, ConfigType.DOUBLE);
        register(entries, "warTornCombatParticlesLow", EchoRegionsConfig.WAR_TORN_COMBAT_PARTICLES_LOW, ConfigType.INT);
        register(entries, "warTornCombatParticlesMed", EchoRegionsConfig.WAR_TORN_COMBAT_PARTICLES_MED, ConfigType.INT);
        register(entries, "warTornCombatParticlesHigh", EchoRegionsConfig.WAR_TORN_COMBAT_PARTICLES_HIGH, ConfigType.INT);
        register(entries, "regionAggroEnabled", EchoRegionsConfig.REGION_AGGRO_ENABLED, ConfigType.BOOLEAN);
        register(entries, "regionAggroCheckIntervalTicks", EchoRegionsConfig.REGION_AGGRO_CHECK_INTERVAL_TICKS, ConfigType.INT);
        register(entries, "regionAggroMaxMobsPerCheck", EchoRegionsConfig.REGION_AGGRO_MAX_MOBS_PER_CHECK, ConfigType.INT);
        register(entries, "hauntedAggroChance", EchoRegionsConfig.HAUNTED_AGGRO_CHANCE, ConfigType.DOUBLE);
        register(entries, "hauntedAggroRadius", EchoRegionsConfig.HAUNTED_AGGRO_RADIUS, ConfigType.INT);
        register(entries, "warTornAggroChance", EchoRegionsConfig.WAR_TORN_AGGRO_CHANCE, ConfigType.DOUBLE);
        register(entries, "warTornAggroRadius", EchoRegionsConfig.WAR_TORN_AGGRO_RADIUS, ConfigType.INT);
        return entries;
    }

    private static void register(
            Map<String, ConfigEntry> entries,
            String key,
            ModConfigSpec.ConfigValue<?> value,
            ConfigType type
    ) {
        entries.put(key.toLowerCase(Locale.ROOT), new ConfigEntry(key, value, type));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("echoregion")
                        .then(Commands.literal("here")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    ServerLevel level = source.getLevel();
                                    long gameTime = level.getGameTime();
                                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                                    RegionPos regionPos = RegionPos.fromChunk(chunkPos);
                                    RegionMemoryData data = RegionMemoryData.get(level);
                                    RegionMemory memory = data.getMemory(regionPos);
                                    RegionState.AggregatedScores local = RegionState.localScores(data, regionPos);
                                    RegionState.AggregatedScores area = RegionState.aggregateAreaScores(data, regionPos);
                                    RegionState.AggregatedScores areaMax = RegionState.aggregateAreaMaxScores(data, regionPos);
                                    RegionState.AggregatedScores areaFloor = RegionState.aggregateAreaFloorScores(data, regionPos);
                                    RegionState.AggregatedScores headlineScores = RegionState.aggregateAreaScoresForHeadline(data, regionPos);
                                    RegionState.HeadlineResult headline = RegionState.resolveHeadline(data, regionPos, headlineScores, gameTime);
                                    RegionState state = headline.state();
                                    RegionState.DominantScore dominant = RegionState.getDominant(headlineScores);
                                    RegionState.Intensity headlineIntensity = RegionState.intensityForState(state, headlineScores);
                                    var tagSnapshots = RegionState.buildTagSnapshots(area, areaMax, areaFloor);
                                    String previousHeadline = memory != null ? memory.getHeadlineId() : "neutral";
                                    boolean headlineChanged = data.updateHeadline(regionPos, state.getId(), gameTime);
                                    long lastUpdated = memory != null ? memory.getLastUpdated() : 0L;
                                    long ticksAgo = lastUpdated > 0 ? Math.max(0, gameTime - lastUpdated) : 0L;
                                    String dimension = level.dimension().identifier().toString();
                                    if (debugEnabled && headlineChanged) {
                                        EchoRegions.LOGGER.info(
                                                "Headline changed for region [{}, {}] in {}: {} -> {} ({})",
                                                regionPos.x(),
                                                regionPos.z(),
                                                dimension,
                                                previousHeadline,
                                                state.getId(),
                                                headline.reason()
                                        );
                                    }

                                    ChatFormatting stateColor = getStateColor(state);

                                    Component header = Component.literal("[EchoRegions]").withStyle(ChatFormatting.AQUA);
                                    Component stateComponent = Component.translatable("echoregions.state." + state.getId()).withStyle(stateColor);

                                    Component message = debugEnabled
                                            ? Component.empty()
                                                    .append(header)
                                                    .append(Component.literal("\n> Region: " + regionPos.x() + ", " + regionPos.z()))
                                                    .append(Component.literal("\n> Dimension: " + dimension))
                                                    .append(Component.literal("\n> Headline: "))
                                                    .append(stateComponent)
                                                    .append(Component.literal(" (" + headlineIntensity.name() + ", " + headline.reason() + ", dominant=" + dominant.getId() + ")"))
                                                    .append(Component.literal("\n> Top tags:"))
                                                    .append(buildTopTags(tagSnapshots))
                                                    .append(Component.literal("\n> Residual:"))
                                                    .append(Component.literal("\n  - enabled=" + EchoRegionsConfig.RESIDUAL_ENABLED.get()))
                                                    .append(Component.literal("\n  - negativePercent=" + EchoRegionsConfig.RESIDUAL_NEGATIVE_PERCENT.get()))
                                                    .append(Component.literal("\n  - positivePercent=" + EchoRegionsConfig.RESIDUAL_POSITIVE_PERCENT.get()))
                                                    .append(Component.literal("\n  - minFloor=" + EchoRegionsConfig.RESIDUAL_MIN_FLOOR.get()))
                                                    .append(Component.literal("\n  - affectsHeadline=" + EchoRegionsConfig.RESIDUAL_AFFECTS_HEADLINE.get()))
                                                    .append(Component.literal("\n> Thresholds:"))
                                                    .append(Component.literal("\n  - mining=" + RegionState.getThresholdMining()))
                                                    .append(Component.literal("\n  - combat=" + RegionState.getThresholdCombat()))
                                                    .append(Component.literal("\n  - death=" + RegionState.getThresholdDeath()))
                                                    .append(Component.literal("\n  - farm=" + RegionState.getThresholdFarm()))
                                                    .append(Component.literal("\n  - build=" + RegionState.getThresholdBuild()))
                                                    .append(Component.literal("\n  - fire=" + RegionState.getThresholdFire()))
                                                    .append(Component.literal("\n  - travel=" + RegionState.getThresholdTravel()))
                                                    .append(Component.literal("\n  - exploit=" + RegionState.getThresholdExploit()))
                                                    .append(Component.literal("\n\nLocal Region:"))
                                                    .append(Component.literal("\n  - mining: " + local.mining()))
                                                    .append(Component.literal("\n  - combat: " + local.combat()))
                                                    .append(Component.literal("\n  - death: " + local.death()))
                                                    .append(Component.literal("\n  - farm: " + local.farm()))
                                                    .append(Component.literal("\n  - build: " + local.build()))
                                                    .append(Component.literal("\n  - fire: " + local.fire()))
                                                    .append(Component.literal("\n  - travel: " + local.travel()))
                                                    .append(Component.literal("\n  - exploit: " + local.exploit()))
                                                    .append(Component.literal("\n\nArea (3x3 regions):"))
                                                    .append(Component.literal("\n  - mining: " + area.mining()))
                                                    .append(Component.literal("\n  - combat: " + area.combat()))
                                                    .append(Component.literal("\n  - death: " + area.death()))
                                                    .append(Component.literal("\n  - farm: " + area.farm()))
                                                    .append(Component.literal("\n  - build: " + area.build()))
                                                    .append(Component.literal("\n  - fire: " + area.fire()))
                                                    .append(Component.literal("\n  - travel: " + area.travel()))
                                                    .append(Component.literal("\n  - exploit: " + area.exploit()))
                                                    .append(Component.literal("\n\nDecay:"))
                                                    .append(Component.literal("\n  - lastUpdated: " + lastUpdated + " (ago " + ticksAgo + " ticks)"))
                                            : Component.empty()
                                                    .append(header)
                                                    .append(Component.literal("\n> Region: " + regionPos.x() + ", " + regionPos.z()))
                                                    .append(Component.literal("\n> Dimension: " + dimension))
                                                    .append(Component.literal("\n> Headline: "))
                                                    .append(stateComponent)
                                                    .append(Component.literal(" (" + headlineIntensity.name() + ")"));
                                    source.sendSuccess(() -> message, false);
                                    return 1;
                                }))
                        .then(configCommand())
                        .then(debugCommand())
                        .then(hudCommand())
                        .then(mapCommand())
                        .then(decayCommand())
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> configCommand() {
        return Commands.literal("config")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(CONFIG_KEY_SUGGESTIONS)
                                .executes(context -> handleGetConfig(context.getSource(),
                                        StringArgumentType.getString(context, "key")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(CONFIG_KEY_SUGGESTIONS)
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(context -> handleSetConfig(context.getSource(),
                                                StringArgumentType.getString(context, "key"),
                                                StringArgumentType.getString(context, "value"))))))
                .then(Commands.literal("reload")
                        .executes(context -> handleReloadConfig(context.getSource())))
                .then(Commands.literal("reset")
                        .executes(context -> handleResetAll(context.getSource()))
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(CONFIG_KEY_SUGGESTIONS)
                                .executes(context -> handleResetOne(context.getSource(),
                                        StringArgumentType.getString(context, "key")))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> debugCommand() {
        return Commands.literal("debug")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("on").executes(context -> {
                    debugEnabled = true;
                    context.getSource().sendSuccess(() -> Component.literal("EchoRegions debug output enabled."), true);
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    debugEnabled = false;
                    context.getSource().sendSuccess(() -> Component.literal("EchoRegions debug output disabled."), true);
                    return 1;
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> hudCommand() {
        return Commands.literal("hud")
                .then(Commands.literal("on").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DebugOverlayTracker.setHudEnabled(player, true);
                    DebugOverlaySync.sendHudUpdate(player);
                    context.getSource().sendSuccess(() -> Component.literal("EchoRegions HUD enabled."), true);
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DebugOverlayTracker.setHudEnabled(player, false);
                    DebugOverlaySync.sendHudDisabled(player);
                    context.getSource().sendSuccess(() -> Component.literal("EchoRegions HUD disabled."), true);
                    return 1;
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> mapCommand() {
        return Commands.literal("map")
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_MAP_RADIUS))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int radius = IntegerArgumentType.getInteger(context, "radius");
                            DebugOverlayTracker.openMap(player, radius);
                            DebugOverlaySync.sendMapUpdate(player);
                            context.getSource().sendSuccess(() -> Component.literal("EchoRegions map opened (radius=" + radius + ")."), true);
                            return 1;
                        }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> decayCommand() {
        return Commands.literal("decay")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("now").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    MinecraftServer server = source.getServer();
                    EchoRegionsConfig.DecayMode mode = EchoRegionsConfig.DECAY_MODE.get();
                    for (ServerLevel level : server.getAllLevels()) {
                        RegionMemoryData data = RegionMemoryData.get(level);
                        if (mode == EchoRegionsConfig.DecayMode.PERCENT) {
                            data.decayAllPercent(
                                    EchoRegionsConfig.NEGATIVE_DECAY_PERCENT.get(),
                                    EchoRegionsConfig.POSITIVE_DECAY_PERCENT.get(),
                                    level.getGameTime()
                            );
                        } else {
                            data.decayAllFlat(
                                    EchoRegionsConfig.NEGATIVE_DECAY_FLAT_AMOUNT.get(),
                                    EchoRegionsConfig.POSITIVE_DECAY_FLAT_AMOUNT.get(),
                                    level.getGameTime()
                            );
                        }
                    }
                    source.sendSuccess(() -> Component.literal("Decay applied."), true);
                    return 1;
                }));
    }

    private static int handleGetConfig(CommandSourceStack source, String keyInput) {
        ConfigEntry entry = getConfigEntry(keyInput);
        if (entry == null) {
            source.sendFailure(Component.literal("Unknown config key: " + keyInput));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(entry.key() + " = " + formatValue(entry)), false);
        return 1;
    }

    private static int handleSetConfig(CommandSourceStack source, String keyInput, String valueInput) {
        ConfigEntry entry = getConfigEntry(keyInput);
        if (entry == null) {
            source.sendFailure(Component.literal("Unknown config key: " + keyInput));
            return 0;
        }
        boolean ok = setConfigValue(entry, valueInput, source);
        if (!ok) {
            return 0;
        }
        EchoRegionsConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal(entry.key() + " set to " + formatValue(entry)), true);
        return 1;
    }

    private static int handleReloadConfig(CommandSourceStack source) {
        if (!tryReloadConfig()) {
            source.sendFailure(Component.literal("Config reload failed (no loaded config present)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Config reloaded."), true);
        return 1;
    }

    private static int handleResetAll(CommandSourceStack source) {
        Set<ModConfigSpec.ConfigValue<?>> uniqueValues = Set.copyOf(CONFIG_KEYS.values().stream()
                .map(ConfigEntry::value)
                .toList());
        for (ModConfigSpec.ConfigValue<?> value : uniqueValues) {
            resetValue(value);
        }
        EchoRegionsConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("Config reset to defaults."), true);
        return 1;
    }

    private static int handleResetOne(CommandSourceStack source, String keyInput) {
        ConfigEntry entry = getConfigEntry(keyInput);
        if (entry == null) {
            source.sendFailure(Component.literal("Unknown config key: " + keyInput));
            return 0;
        }
        resetValue(entry.value());
        EchoRegionsConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal(entry.key() + " reset to default (" + entry.value().getDefault() + ")."), true);
        return 1;
    }

    private static void resetValue(ModConfigSpec.ConfigValue<?> value) {
        Object def = value.getDefault();
        setRawValue(value, def);
    }

    private static boolean tryReloadConfig() {
        try {
            Field field = ModConfigSpec.class.getDeclaredField("loadedConfig");
            field.setAccessible(true);
            Object loadedConfig = field.get(EchoRegionsConfig.SPEC);
            if (loadedConfig == null) {
                return false;
            }
            Method load = loadedConfig.getClass().getMethod("load");
            load.invoke(loadedConfig);
            Class<?> loadedConfigClass = Class.forName("net.neoforged.fml.config.IConfigSpec$ILoadedConfig");
            Method accept = ModConfigSpec.class.getMethod("acceptConfig", loadedConfigClass);
            accept.invoke(EchoRegionsConfig.SPEC, loadedConfig);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean setConfigValue(ConfigEntry entry, String valueInput, CommandSourceStack source) {
        try {
            switch (entry.type()) {
                case INT -> {
                    int value = Integer.parseInt(valueInput);
                    setRawValue(entry.value(), value);
                }
                case DOUBLE -> {
                    double value = Double.parseDouble(valueInput);
                    setRawValue(entry.value(), value);
                }
                case ENUM -> {
                    if (entry.value() == EchoRegionsConfig.DECAY_MODE) {
                        EchoRegionsConfig.DecayMode mode = EchoRegionsConfig.DecayMode.valueOf(valueInput.toUpperCase(Locale.ROOT));
                        setRawValue(entry.value(), mode);
                    } else {
                        source.sendFailure(Component.literal("Unsupported enum key: " + entry.key()));
                        return false;
                    }
                }
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(valueInput) && !"false".equalsIgnoreCase(valueInput)) {
                        source.sendFailure(Component.literal("Invalid boolean for " + entry.key() + ": " + valueInput));
                        return false;
                    }
                    boolean value = Boolean.parseBoolean(valueInput);
                    setRawValue(entry.value(), value);
                }
                default -> {
                    source.sendFailure(Component.literal("Unsupported config type for key: " + entry.key()));
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid value for " + entry.key() + ": " + valueInput));
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setRawValue(ModConfigSpec.ConfigValue<?> value, Object newValue) {
        ((ModConfigSpec.ConfigValue<Object>) value).set(newValue);
    }

    private static String formatValue(ConfigEntry entry) {
        Object value = entry.value().get();
        return Objects.toString(value);
    }

    private static ConfigEntry getConfigEntry(String keyInput) {
        if (keyInput == null) {
            return null;
        }
        return CONFIG_KEYS.get(keyInput.toLowerCase(Locale.ROOT));
    }

    private static ChatFormatting getStateColor(RegionState state) {
        return switch (state) {
            case SCARRED -> ChatFormatting.DARK_RED;
            case HAUNTED -> ChatFormatting.DARK_PURPLE;
            case WAR_TORN -> ChatFormatting.GOLD;
            case NEUTRAL -> ChatFormatting.GRAY;
            case CULTIVATED -> ChatFormatting.GREEN;
            case SETTLED -> ChatFormatting.DARK_GREEN;
            case BLIGHTED -> ChatFormatting.RED;
            case TRAVELLED -> ChatFormatting.BLUE;
            case EXPLOITED -> ChatFormatting.DARK_AQUA;
        };
    }

    private static Component buildTopTags(java.util.List<RegionState.TagSnapshot> tags) {
        if (tags.isEmpty()) {
            return Component.literal("\n  - none");
        }
        java.util.List<RegionState.TagSnapshot> sorted = tags.stream()
                .sorted((a, b) -> Integer.compare(b.current(), a.current()))
                .toList();
        int shown = Math.min(3, sorted.size());
        MutableComponent component = Component.empty();
        for (int i = 0; i < shown; i++) {
            RegionState.TagSnapshot tag = sorted.get(i);
            Component stateName = Component.translatable("echoregions.state." + tag.state().getId())
                    .withStyle(getStateColor(tag.state()));
            component.append(Component.literal("\n  - "))
                    .append(stateName)
                    .append(Component.literal(" (" + tag.intensity().name()
                            + ", current=" + tag.current()
                            + ", max=" + tag.max()
                            + ", floor=" + tag.floor()
                            + ")"));
        }
        int remaining = sorted.size() - shown;
        if (remaining > 0) {
            component.append(Component.literal("\n  ...+" + remaining));
        }
        return component;
    }
}
