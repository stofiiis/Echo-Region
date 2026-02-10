package cz.stofiiis.echoregions.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cz.stofiiis.echoregions.commands.EchoRegionCommand;
import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
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
        RegionMemoryData data = RegionMemoryData.get(level);
        if (trackMining) {
            data.addMiningScore(chunkPos, 1, level.getGameTime());
        }
        if (trackExploit) {
            data.addExploitScore(chunkPos, 1, level.getGameTime());
        }
        if (trackFarm) {
            data.addFarmScore(chunkPos, 1, level.getGameTime());
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
            data.addCombatScore(entity.chunkPosition(), 1, level.getGameTime());
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            data.addDeathScore(entity.chunkPosition(), 1, level.getGameTime());
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
        RegionMemoryData data = RegionMemoryData.get(level);
        data.addFarmScore(chunkPos, 2, level.getGameTime());
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
                RegionMemoryData data = RegionMemoryData.get(level);
                data.addFireScore(chunkPos, 1, level.getGameTime());
            }
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        RegionMemoryData data = RegionMemoryData.get(level);
        if (isPlantedCrop(placed)) {
            data.addFarmScore(chunkPos, 1, level.getGameTime());
            return;
        }
        if (isSapling(placed)) {
            return;
        }
        data.addBuildScore(chunkPos, 1, level.getGameTime());
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos center = BlockPos.containing(event.getExplosion().center());
        ChunkPos chunkPos = new ChunkPos(center);
        RegionMemoryData data = RegionMemoryData.get(level);
        data.addFireScore(chunkPos, 1, level.getGameTime());
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
        if (current.equals(previous)) {
            return;
        }
        lastChunkByPlayer.put(serverPlayer.getUUID(), current);
        RegionMemoryData data = RegionMemoryData.get(level);
        data.addTravelScore(current.pos(), 1, level.getGameTime());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastChunkByPlayer.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int intervalTicks = Math.max(1, EchoRegionsConfig.DECAY_INTERVAL_MINUTES.get()) * 20 * 60;
        if (++tickCounter < intervalTicks) {
            return;
        }
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        EchoRegionsConfig.DecayMode mode = EchoRegionsConfig.DECAY_MODE.get();
        for (ServerLevel level : server.getAllLevels()) {
            RegionMemoryData data = RegionMemoryData.get(level);
            if (mode == EchoRegionsConfig.DecayMode.PERCENT) {
                data.decayAllPercent(EchoRegionsConfig.DECAY_PERCENT.get(), level.getGameTime());
            } else {
                data.decayAllFlat(EchoRegionsConfig.DECAY_FLAT_AMOUNT.get(), level.getGameTime());
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
}
