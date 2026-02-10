package cz.stofiiis.echoregions.events;

import cz.stofiiis.echoregions.commands.EchoRegionCommand;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class RegionEvents {
    private static final int DECAY_INTERVAL_TICKS = 20 * 60 * 5;
    private int tickCounter = 0;

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
        if (!isRelevantMiningBlock(state)) {
            // allow other block break tracking below
        } else {
            ChunkPos chunkPos = new ChunkPos(event.getPos());
            RegionMemoryData data = RegionMemoryData.get(level);
            data.addMiningScore(chunkPos, 1, level.getGameTime());
        }
        if (isMatureCrop(state)) {
            ChunkPos chunkPos = new ChunkPos(event.getPos());
            RegionMemoryData data = RegionMemoryData.get(level);
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
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (!isPlantedCrop(placed)) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        RegionMemoryData data = RegionMemoryData.get(level);
        data.addFarmScore(chunkPos, 1, level.getGameTime());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < DECAY_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            RegionMemoryData data = RegionMemoryData.get(level);
            data.decayAll(level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EchoRegionCommand.register(event.getDispatcher());
    }

    private static boolean isRelevantMiningBlock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES);
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

    private static boolean isBonemealFarmingTarget(BlockState state) {
        return state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock;
    }
}
