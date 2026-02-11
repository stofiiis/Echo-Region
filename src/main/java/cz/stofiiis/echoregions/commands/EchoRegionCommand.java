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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import cz.stofiiis.echoregions.config.EchoRegionsConfig;
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

    private enum ConfigType {
        INT,
        DOUBLE,
        ENUM
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
        register(entries, "scarredPebbleChance", EchoRegionsConfig.SCARRED_PEBBLE_CHANCE, ConfigType.DOUBLE);
        register(entries, "hauntedEctoplasmChance", EchoRegionsConfig.HAUNTED_ECTOPLASM_CHANCE, ConfigType.DOUBLE);
        register(entries, "warTornStrengthChance", EchoRegionsConfig.WAR_TORN_STRENGTH_CHANCE, ConfigType.DOUBLE);
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
                                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                                    RegionPos regionPos = RegionPos.fromChunk(chunkPos);
                                    RegionMemoryData data = RegionMemoryData.get(level);
                                    RegionMemory memory = data.getMemory(regionPos);
                                    RegionState.AggregatedScores local = RegionState.localScores(data, regionPos);
                                    RegionState.AggregatedScores area = RegionState.aggregateAreaScores(data, regionPos);
                                    RegionState.HeadlineResult headline = RegionState.resolveHeadline(data, regionPos, area);
                                    RegionState state = headline.state();
                                    RegionState.DominantScore dominant = RegionState.getDominant(area);
                                    var activeTags = RegionState.getActiveTags(area);
                                    data.updateHeadline(regionPos, state.getId());
                                    long lastUpdated = memory != null ? memory.getLastUpdated() : 0L;
                                    long ticksAgo = lastUpdated > 0 ? Math.max(0, level.getGameTime() - lastUpdated) : 0L;
                                    String dimension = level.dimension().identifier().toString();

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
                                                    .append(Component.literal(" (" + headline.reason() + ", dominant=" + dominant.getId() + ")"))
                                                    .append(Component.literal("\n> Active tags:"))
                                                    .append(buildActiveTags(activeTags))
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
                                                    .append(Component.literal(" (" + headline.reason() + ", dominant=" + dominant.getId() + ")"));
                                    source.sendSuccess(() -> message, false);
                                    return 1;
                                }))
                        .then(configCommand())
                        .then(debugCommand())
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

    private static Component buildActiveTags(java.util.List<RegionState.ActiveTag> tags) {
        if (tags.isEmpty()) {
            return Component.literal("\n  - none");
        }
        java.util.List<RegionState.ActiveTag> sorted = tags.stream()
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();
        int shown = Math.min(3, sorted.size());
        MutableComponent component = Component.empty();
        for (int i = 0; i < shown; i++) {
            RegionState.ActiveTag tag = sorted.get(i);
            Component stateName = Component.translatable("echoregions.state." + tag.state().getId())
                    .withStyle(getStateColor(tag.state()));
            component.append(Component.literal("\n  - "))
                    .append(stateName)
                    .append(Component.literal(" (" + tag.intensity().name() + ", score=" + tag.score() + ")"));
        }
        int remaining = sorted.size() - shown;
        if (remaining > 0) {
            component.append(Component.literal("\n  ...+" + remaining));
        }
        return component;
    }
}
