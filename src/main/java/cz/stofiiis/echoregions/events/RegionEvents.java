package cz.stofiiis.echoregions.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cz.stofiiis.echoregions.EchoRegions;
import cz.stofiiis.echoregions.commands.EchoRegionCommand;
import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import cz.stofiiis.echoregions.data.RegionPos;
import cz.stofiiis.echoregions.debug.DebugOverlaySync;
import cz.stofiiis.echoregions.debug.DebugOverlayTracker;
import cz.stofiiis.echoregions.region.RegionState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class RegionEvents {
    private static final TagKey<Block> BUILDING_BLOCKS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(EchoRegions.MOD_ID, "building_blocks")
    );
    private static final int AMBIENT_CHECK_INTERVAL_TICKS = 40;

    private final Map<UUID, PlayerChunk> lastChunkByPlayer = new HashMap<>();
    private int tickCounter = 0;

    private record PlayerChunk(ResourceKey<Level> dimension, ChunkPos pos) {
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        boolean trackMining = isRelevantMiningBlock(state);
        boolean trackFarm = isMatureCrop(state);
        boolean trackExploit = isExploitBlock(state);
        if (!(trackMining || trackFarm || trackExploit)) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        RegionPos regionPos = RegionPos.fromChunk(chunkPos);
        RegionMemoryData data = RegionMemoryData.get(level);
        if (trackMining) {
            data.addMiningScore(regionPos, 1, level.getGameTime());
        }
        if (trackExploit) {
            data.addExploitScore(regionPos, 1, level.getGameTime());
        }
        if (trackFarm) {
            data.addFarmScore(regionPos, 1, level.getGameTime());
        }
        if (trackMining) {
            RegionState.ActiveTag scarred = getActiveTag(data, regionPos, RegionState.SCARRED);
            if (scarred != null && roll(level, chanceForIntensity(scarred.intensity(), 0.15f, 0.25f, 0.4f))) {
                spawnDust(level, event.getPos(), countForIntensity(scarred.intensity(), 3, 5, 7));
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        RegionMemoryData data = RegionMemoryData.get(level);
        if (entity instanceof Monster) {
            RegionPos regionPos = RegionPos.fromChunk(entity.chunkPosition());
            data.addCombatScore(regionPos, 1, level.getGameTime());
            RegionState.ActiveTag warTorn = getActiveTag(data, regionPos, RegionState.WAR_TORN);
            if (warTorn != null && roll(level, chanceForIntensity(warTorn.intensity(), 0.2f, 0.35f, 0.5f))) {
                spawnBattleDust(level, entity.blockPosition(), countForIntensity(warTorn.intensity(), 4, 6, 9));
            }
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            data.addDeathScore(RegionPos.fromChunk(entity.chunkPosition()), 1, level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onBonemeal(BonemealEvent event) {
        if (event.isCanceled() && !event.isSuccessful()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (!event.isValidBonemealTarget()) {
            return;
        }
        if (!isBonemealFarmingTarget(event.getState())) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        RegionPos regionPos = RegionPos.fromChunk(chunkPos);
        RegionMemoryData data = RegionMemoryData.get(level);
        data.addFarmScore(regionPos, 2, level.getGameTime());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel level)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (placed.getBlock() instanceof BaseFireBlock && !(event.getEntity() instanceof Player)) {
            BlockState replaced = event.getBlockSnapshot().getState();
            if (!replaced.isAir()) {
                ChunkPos chunkPos = new ChunkPos(event.getPos());
                RegionPos regionPos = RegionPos.fromChunk(chunkPos);
                RegionMemoryData data = RegionMemoryData.get(level);
                data.addFireScore(regionPos, 1, level.getGameTime());
            }
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        RegionPos regionPos = RegionPos.fromChunk(chunkPos);
        RegionMemoryData data = RegionMemoryData.get(level);
        if (isPlantedCrop(placed)) {
            data.addFarmScore(regionPos, 1, level.getGameTime());
            return;
        }
        if (isSapling(placed)) {
            return;
        }
        if (isBuildBlock(placed)) {
            data.addBuildScore(regionPos, 1, level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = BlockPos.containing(event.getExplosion().center());
        ChunkPos chunkPos = new ChunkPos(center);
        RegionPos regionPos = RegionPos.fromChunk(chunkPos);
        RegionMemoryData data = RegionMemoryData.get(level);
        data.addFireScore(regionPos, 1, level.getGameTime());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = (ServerLevel) serverPlayer.level();
        PlayerChunk current = new PlayerChunk(level.dimension(), serverPlayer.chunkPosition());
        PlayerChunk previous = lastChunkByPlayer.get(serverPlayer.getUUID());
        if (!current.equals(previous)) {
            lastChunkByPlayer.put(serverPlayer.getUUID(), current);
            RegionMemoryData data = RegionMemoryData.get(level);
            data.addTravelScore(RegionPos.fromChunk(current.pos()), 1, level.getGameTime());
        }
        handleHauntedAmbient(level, serverPlayer, current.pos());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastChunkByPlayer.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DebugOverlayTracker.clear(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        DebugOverlaySync.tick(server);

        int intervalTicks = Math.max(1, EchoRegionsConfig.DECAY_INTERVAL_MINUTES.get()) * 20 * 60;
        if (++tickCounter < intervalTicks) {
            return;
        }
        tickCounter = 0;
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
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EchoRegionCommand.register(event.getDispatcher());
    }

    private static boolean isRelevantMiningBlock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES);
    }

    private static boolean isExploitBlock(BlockState state) {
        return state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    private static boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
        }
        return false;
    }

    private static boolean isPlantedCrop(BlockState state) {
        return state.is(BlockTags.CROPS)
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock;
    }

    private static boolean isSapling(BlockState state) {
        return state.is(BlockTags.SAPLINGS);
    }

    private static boolean isBonemealFarmingTarget(BlockState state) {
        return state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock;
    }

    private static boolean isBuildBlock(BlockState state) {
        return state.is(BUILDING_BLOCKS);
    }

    private static void handleHauntedAmbient(ServerLevel level, ServerPlayer player, ChunkPos chunkPos) {
        if (!isNight(level)) {
            return;
        }
        if (level.getGameTime() % AMBIENT_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        RegionPos regionPos = RegionPos.fromChunk(chunkPos);
        RegionMemoryData data = RegionMemoryData.get(level);
        RegionState.ActiveTag haunted = getActiveTag(data, regionPos, RegionState.HAUNTED);
        if (haunted == null) {
            return;
        }
        float chance = chanceForIntensity(haunted.intensity(), 0.05f, 0.1f, 0.2f);
        if (!roll(level, chance)) {
            return;
        }
        BlockPos base = player.blockPosition();
        double x = base.getX() + 0.5 + level.random.nextGaussian() * 1.5;
        double y = base.getY() + 1.0 + level.random.nextDouble() * 0.5;
        double z = base.getZ() + 0.5 + level.random.nextGaussian() * 1.5;
        int count = countForIntensity(haunted.intensity(), 2, 4, 6);
        level.sendParticles(ParticleTypes.SOUL, x, y, z, count, 0.2, 0.2, 0.2, 0.01);
        if (roll(level, chance * 0.5f)) {
            level.playSound(null, base, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.35f, 0.8f + level.random.nextFloat() * 0.2f);
        }
    }

    private static RegionState.ActiveTag getActiveTag(RegionMemoryData data, RegionPos regionPos, RegionState target) {
        RegionState.AggregatedScores area = RegionState.aggregateAreaScores(data, regionPos);
        for (RegionState.ActiveTag tag : RegionState.getActiveTags(area)) {
            if (tag.state() == target) {
                return tag;
            }
        }
        return null;
    }

    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    private static boolean roll(ServerLevel level, float chance) {
        return level.random.nextFloat() < chance;
    }

    private static float chanceForIntensity(RegionState.Intensity intensity, float low, float med, float high) {
        return switch (intensity) {
            case LOW -> low;
            case MED -> med;
            case HIGH -> high;
        };
    }

    private static int countForIntensity(RegionState.Intensity intensity, int low, int med, int high) {
        return switch (intensity) {
            case LOW -> low;
            case MED -> med;
            case HIGH -> high;
        };
    }

    private static void spawnDust(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, count, 0.3, 0.2, 0.3, 0.01);
    }

    private static void spawnBattleDust(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, count, 0.3, 0.2, 0.3, 0.05);
    }
}
