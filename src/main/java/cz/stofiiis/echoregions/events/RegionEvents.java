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
import cz.stofiiis.echoregions.registry.EchoRegionsItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class RegionEvents {
    private static final int WAR_TORN_STRENGTH_DURATION_TICKS = 200;

    private static final TagKey<Block> BUILDING_BLOCKS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(EchoRegions.MOD_ID, "building_blocks")
    );
    private final Map<UUID, PlayerChunk> lastChunkByPlayer = new HashMap<>();
    private final Map<UUID, AmbientCooldowns> ambientCooldowns = new HashMap<>();
    private int tickCounter = 0;

    private record PlayerChunk(ResourceKey<Level> dimension, ChunkPos pos) {
    }

    private static final class AmbientCooldowns {
        private long lastScarred;
        private long lastHaunted;
        private long lastWarTorn;
        private long lastSound;
        private long lastAggro;
    }

    private record HeadlineContext(RegionState state, RegionState.Intensity intensity) {
    }

    private record AggroProfile(double chance, int radius) {
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
        RegionState.ActiveTag scarred = null;
        if (trackMining) {
            scarred = getActiveTag(data, regionPos, RegionState.SCARRED);
            if (scarred != null
                    && canReceiveScarredBonus(event.getPlayer())
                    && roll(level, EchoRegionsConfig.SCARRED_PEBBLE_CHANCE.get())) {
                spawnScarredBonusDrop(level, event.getPos());
            }
        }
        if (trackMining && EchoRegionsConfig.AMBIENT_ENABLED.get() && scarred != null) {
            if (roll(level, chanceForIntensity(
                    scarred.intensity(),
                    EchoRegionsConfig.SCARRED_MINING_CHANCE_LOW.get(),
                    EchoRegionsConfig.SCARRED_MINING_CHANCE_MED.get(),
                    EchoRegionsConfig.SCARRED_MINING_CHANCE_HIGH.get()
            ))) {
                spawnDust(level, event.getPos(), countForIntensity(
                        scarred.intensity(),
                        EchoRegionsConfig.SCARRED_MINING_PARTICLES_LOW.get(),
                        EchoRegionsConfig.SCARRED_MINING_PARTICLES_MED.get(),
                        EchoRegionsConfig.SCARRED_MINING_PARTICLES_HIGH.get()
                ));
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
            boolean killedByPlayer = event.getSource().getEntity() instanceof Player killer
                    && !killer.getAbilities().instabuild;
            if (killedByPlayer) {
                RegionState.ActiveTag haunted = getActiveTag(data, regionPos, RegionState.HAUNTED);
                if (haunted != null && roll(level, EchoRegionsConfig.HAUNTED_ECTOPLASM_CHANCE.get())) {
                    entity.spawnAtLocation(level, new ItemStack(EchoRegionsItems.ECTOPLASM.get()));
                }
            }
            if (EchoRegionsConfig.AMBIENT_ENABLED.get()) {
                RegionState.ActiveTag warTorn = getActiveTag(data, regionPos, RegionState.WAR_TORN);
                if (warTorn != null && roll(level, chanceForIntensity(
                        warTorn.intensity(),
                        EchoRegionsConfig.WAR_TORN_COMBAT_CHANCE_LOW.get(),
                        EchoRegionsConfig.WAR_TORN_COMBAT_CHANCE_MED.get(),
                        EchoRegionsConfig.WAR_TORN_COMBAT_CHANCE_HIGH.get()
                ))) {
                    spawnBattleDust(level, entity.blockPosition(), countForIntensity(
                            warTorn.intensity(),
                            EchoRegionsConfig.WAR_TORN_COMBAT_PARTICLES_LOW.get(),
                            EchoRegionsConfig.WAR_TORN_COMBAT_PARTICLES_MED.get(),
                            EchoRegionsConfig.WAR_TORN_COMBAT_PARTICLES_HIGH.get()
                    ));
                }
            }
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            data.addDeathScore(RegionPos.fromChunk(entity.chunkPosition()), 1, level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Monster)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RegionPos regionPos = RegionPos.fromChunk(new ChunkPos(BlockPos.containing(event.getX(), event.getY(), event.getZ())));
        RegionMemoryData data = RegionMemoryData.get(level);
        RegionState.ActiveTag warTorn = getActiveTag(data, regionPos, RegionState.WAR_TORN);
        if (warTorn == null || !roll(level, EchoRegionsConfig.WAR_TORN_STRENGTH_CHANCE.get())) {
            return;
        }
        if (mob.hasEffect(MobEffects.STRENGTH)) {
            return;
        }
        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, WAR_TORN_STRENGTH_DURATION_TICKS, 0));
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
        RegionPos currentRegion = RegionPos.fromChunk(current.pos());
        if (!current.equals(previous)) {
            boolean regionChanged = hasRegionChanged(previous, current);
            lastChunkByPlayer.put(serverPlayer.getUUID(), current);
            RegionMemoryData data = RegionMemoryData.get(level);
            data.addTravelScore(RegionPos.fromChunk(current.pos()), 1, level.getGameTime());
            if (regionChanged) {
                HeadlineContext headline = resolveHeadline(level, currentRegion, level.getGameTime());
                AmbientCooldowns cooldowns = ambientCooldowns.computeIfAbsent(serverPlayer.getUUID(), id -> new AmbientCooldowns());
                triggerEntryAmbient(level, serverPlayer, headline, cooldowns, level.getGameTime());
                triggerRegionEntryFeedback(level, serverPlayer, headline, cooldowns, level.getGameTime());
            }
        }
        handleAmbient(level, serverPlayer, current.pos(), currentRegion);
        handleAggro(level, serverPlayer, currentRegion);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastChunkByPlayer.remove(event.getEntity().getUUID());
        ambientCooldowns.remove(event.getEntity().getUUID());
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

    private void handleAmbient(ServerLevel level, ServerPlayer player, ChunkPos chunkPos, RegionPos regionPos) {
        if (!EchoRegionsConfig.AMBIENT_ENABLED.get()) {
            return;
        }
        int interval = Math.max(1, EchoRegionsConfig.AMBIENT_CHECK_INTERVAL_TICKS.get());
        long gameTime = level.getGameTime();
        if (gameTime % interval != 0) {
            return;
        }

        HeadlineContext headline = resolveHeadline(level, regionPos, gameTime);
        AmbientCooldowns cooldowns = ambientCooldowns.computeIfAbsent(player.getUUID(), id -> new AmbientCooldowns());
        switch (headline.state()) {
            case SCARRED -> triggerScarredAmbient(level, player, headline.intensity(), cooldowns, gameTime);
            case HAUNTED -> triggerHauntedAmbient(level, player, headline.intensity(), cooldowns, gameTime);
            case WAR_TORN -> triggerWarTornAmbient(level, player, headline.intensity(), cooldowns, gameTime);
            default -> {
            }
        }
    }

    private void handleAggro(ServerLevel level, ServerPlayer player, RegionPos regionPos) {
        if (!EchoRegionsConfig.REGION_AGGRO_ENABLED.get() || player.isSpectator()) {
            return;
        }
        AmbientCooldowns cooldowns = ambientCooldowns.computeIfAbsent(player.getUUID(), id -> new AmbientCooldowns());
        int interval = Math.max(1, EchoRegionsConfig.REGION_AGGRO_CHECK_INTERVAL_TICKS.get());
        long gameTime = level.getGameTime();
        if (gameTime - cooldowns.lastAggro < interval) {
            return;
        }
        cooldowns.lastAggro = gameTime;

        HeadlineContext headline = resolveHeadline(level, regionPos, gameTime);
        AggroProfile profile = aggroProfileFor(headline.state());
        if (profile == null || profile.chance() <= 0.0 || profile.radius() <= 0) {
            return;
        }
        double chance = Math.min(1.0, profile.chance() * intensityMultiplier(headline.intensity()));
        int maxMobs = Math.max(0, EchoRegionsConfig.REGION_AGGRO_MAX_MOBS_PER_CHECK.get());
        if (maxMobs == 0 || chance <= 0.0) {
            return;
        }

        AABB bounds = player.getBoundingBox().inflate(profile.radius());
        java.util.List<Monster> monsters = level.getEntitiesOfClass(Monster.class, bounds, mob -> isAggroCandidate(mob, player));
        int redirected = 0;
        for (Monster monster : monsters) {
            if (redirected >= maxMobs) {
                break;
            }
            if (!roll(level, chance)) {
                continue;
            }
            monster.setTarget(player);
            monster.setLastHurtByMob(player);
            redirected++;
        }
    }

    private static boolean hasRegionChanged(PlayerChunk previous, PlayerChunk current) {
        if (previous == null) {
            return true;
        }
        if (!previous.dimension().equals(current.dimension())) {
            return true;
        }
        RegionPos prevRegion = RegionPos.fromChunk(previous.pos());
        RegionPos currentRegion = RegionPos.fromChunk(current.pos());
        return prevRegion.x() != currentRegion.x() || prevRegion.z() != currentRegion.z();
    }

    private HeadlineContext resolveHeadline(ServerLevel level, RegionPos regionPos, long gameTime) {
        RegionMemoryData data = RegionMemoryData.get(level);
        RegionState.AggregatedScores headlineScores = RegionState.aggregateAreaScoresForHeadline(data, regionPos);
        RegionState.HeadlineResult headline = RegionState.resolveHeadline(data, regionPos, headlineScores, gameTime);
        RegionState state = headline.state();
        RegionState.Intensity intensity = RegionState.intensityForState(state, headlineScores);
        data.updateHeadline(regionPos, state.getId(), gameTime);
        return new HeadlineContext(state, intensity);
    }

    private void triggerEntryAmbient(
            ServerLevel level,
            ServerPlayer player,
            HeadlineContext headline,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!EchoRegionsConfig.AMBIENT_ENTRY_ENABLED.get()) {
            return;
        }
        switch (headline.state()) {
            case SCARRED -> triggerEntryScarred(level, player, headline.intensity(), cooldowns, gameTime);
            case HAUNTED -> triggerEntryHaunted(level, player, headline.intensity(), cooldowns, gameTime);
            case WAR_TORN -> triggerEntryWarTorn(level, player, headline.intensity(), cooldowns, gameTime);
            default -> {
            }
        }
    }

    private void triggerRegionEntryFeedback(
            ServerLevel level,
            ServerPlayer player,
            HeadlineContext headline,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!EchoRegionsConfig.REGION_ENTRY_FEEDBACK_ENABLED.get()) {
            return;
        }
        sendEntryActionbar(player, headline);
        applyEntryAura(player, headline);
        playEntryStinger(level, player.blockPosition(), headline, cooldowns, gameTime);
    }

    private void sendEntryActionbar(ServerPlayer player, HeadlineContext headline) {
        Component stateName = Component.translatable("echoregions.state." + headline.state().getId())
                .withStyle(stateColor(headline.state()));
        Component message = Component.empty()
                .append(Component.literal("[EchoRegions] ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Entered: ").withStyle(ChatFormatting.GRAY))
                .append(stateName)
                .append(Component.literal(" [" + headline.intensity().name() + "]").withStyle(ChatFormatting.DARK_GRAY));
        player.displayClientMessage(message, true);
    }

    private void applyEntryAura(ServerPlayer player, HeadlineContext headline) {
        int duration = Math.max(0, EchoRegionsConfig.REGION_ENTRY_AURA_DURATION_TICKS.get());
        if (duration <= 0) {
            return;
        }
        MobEffectInstance aura = switch (headline.state()) {
            case SCARRED -> new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 0);
            case HAUNTED -> new MobEffectInstance(MobEffects.WEAKNESS, duration, 0);
            case WAR_TORN -> new MobEffectInstance(MobEffects.STRENGTH, duration, 0);
            default -> null;
        };
        if (aura != null) {
            player.addEffect(aura);
        }
    }

    private void playEntryStinger(
            ServerLevel level,
            BlockPos pos,
            HeadlineContext headline,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        SoundEvent sound = switch (headline.state()) {
            case SCARRED -> SoundEvents.STONE_HIT;
            case HAUNTED -> SoundEvents.AMBIENT_CAVE.value();
            case WAR_TORN -> SoundEvents.SHIELD_BLOCK.value();
            default -> null;
        };
        if (sound == null) {
            return;
        }
        float volume = (float) switch (headline.intensity()) {
            case LOW -> 0.55;
            case MED -> 0.68;
            case HIGH -> 0.8;
        };
        float pitch = switch (headline.state()) {
            case HAUNTED -> 0.72F;
            case WAR_TORN -> 0.9F;
            default -> 0.95F;
        };
        playAmbientSound(level, pos, sound, volume, pitch, cooldowns, gameTime);
    }

    private void triggerEntryScarred(
            ServerLevel level,
            ServerPlayer player,
            RegionState.Intensity intensity,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!cooldownReady(gameTime, cooldowns.lastScarred)) {
            return;
        }
        double multiplier = EchoRegionsConfig.AMBIENT_ENTRY_CHANCE_MULTIPLIER.get();
        double chance = Math.min(1.0, chanceForIntensity(
                intensity,
                EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_LOW.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_MED.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_HIGH.get()
        ) * multiplier);
        if (!roll(level, chance)) {
            return;
        }
        int count = scaleCount(countForIntensity(
                intensity,
                EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_LOW.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_MED.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_HIGH.get()
        ), EchoRegionsConfig.AMBIENT_ENTRY_PARTICLE_MULTIPLIER.get());
        BlockPos base = player.blockPosition();
        spawnDust(level, base, count);
        double soundChance = Math.min(1.0, chanceForIntensity(
                intensity,
                EchoRegionsConfig.SCARRED_SOUND_CHANCE_LOW.get(),
                EchoRegionsConfig.SCARRED_SOUND_CHANCE_MED.get(),
                EchoRegionsConfig.SCARRED_SOUND_CHANCE_HIGH.get()
        ) * EchoRegionsConfig.AMBIENT_ENTRY_SOUND_MULTIPLIER.get());
        if (roll(level, soundChance)) {
            playAmbientSound(
                    level,
                    base,
                    SoundEvents.STONE_HIT,
                    (float) (double) EchoRegionsConfig.SCARRED_SOUND_VOLUME.get(),
                    (float) (double) EchoRegionsConfig.SCARRED_SOUND_PITCH.get(),
                    cooldowns,
                    gameTime
            );
        }
        cooldowns.lastScarred = gameTime;
    }

    private void triggerEntryHaunted(
            ServerLevel level,
            ServerPlayer player,
            RegionState.Intensity intensity,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!isNight(level)) {
            return;
        }
        if (!cooldownReady(gameTime, cooldowns.lastHaunted)) {
            return;
        }
        double chance = Math.min(1.0, chanceForIntensity(
                intensity,
                EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_LOW.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_MED.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_HIGH.get()
        ) * EchoRegionsConfig.AMBIENT_ENTRY_CHANCE_MULTIPLIER.get());
        if (!roll(level, chance)) {
            return;
        }
        int count = scaleCount(countForIntensity(
                intensity,
                EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_LOW.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_MED.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_HIGH.get()
        ), EchoRegionsConfig.AMBIENT_ENTRY_PARTICLE_MULTIPLIER.get());
        BlockPos base = player.blockPosition();
        if (count > 0) {
            level.sendParticles(ParticleTypes.SOUL, base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5, count, 0.35, 0.35, 0.35, 0.01);
        }
        double soundChance = Math.min(1.0, chanceForIntensity(
                intensity,
                EchoRegionsConfig.HAUNTED_SOUND_CHANCE_LOW.get(),
                EchoRegionsConfig.HAUNTED_SOUND_CHANCE_MED.get(),
                EchoRegionsConfig.HAUNTED_SOUND_CHANCE_HIGH.get()
        ) * EchoRegionsConfig.AMBIENT_ENTRY_SOUND_MULTIPLIER.get());
        if (roll(level, soundChance)) {
            playAmbientSound(
                    level,
                    base,
                    SoundEvents.AMBIENT_CAVE.value(),
                    (float) (double) EchoRegionsConfig.HAUNTED_SOUND_VOLUME.get(),
                    (float) (double) EchoRegionsConfig.HAUNTED_SOUND_PITCH.get(),
                    cooldowns,
                    gameTime
            );
        }
        cooldowns.lastHaunted = gameTime;
    }

    private void triggerEntryWarTorn(
            ServerLevel level,
            ServerPlayer player,
            RegionState.Intensity intensity,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!cooldownReady(gameTime, cooldowns.lastWarTorn)) {
            return;
        }
        double chance = Math.min(1.0, chanceForIntensity(
                intensity,
                EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_LOW.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_MED.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_HIGH.get()
        ) * EchoRegionsConfig.AMBIENT_ENTRY_CHANCE_MULTIPLIER.get());
        if (!roll(level, chance)) {
            return;
        }
        int count = scaleCount(countForIntensity(
                intensity,
                EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_LOW.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_MED.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_HIGH.get()
        ), EchoRegionsConfig.AMBIENT_ENTRY_PARTICLE_MULTIPLIER.get());
        BlockPos base = player.blockPosition();
        spawnBattleDust(level, base, count);
        double soundChance = Math.min(1.0, chanceForIntensity(
                intensity,
                EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_LOW.get(),
                EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_MED.get(),
                EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_HIGH.get()
        ) * EchoRegionsConfig.AMBIENT_ENTRY_SOUND_MULTIPLIER.get());
        if (roll(level, soundChance)) {
            playAmbientSound(
                    level,
                    base,
                    SoundEvents.SHIELD_BLOCK.value(),
                    (float) (double) EchoRegionsConfig.WAR_TORN_SOUND_VOLUME.get(),
                    (float) (double) EchoRegionsConfig.WAR_TORN_SOUND_PITCH.get(),
                    cooldowns,
                    gameTime
            );
        }
        cooldowns.lastWarTorn = gameTime;
    }

    private void triggerScarredAmbient(
            ServerLevel level,
            ServerPlayer player,
            RegionState.Intensity intensity,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!cooldownReady(gameTime, cooldowns.lastScarred)) {
            return;
        }
        double chance = chanceForIntensity(
                intensity,
                EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_LOW.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_MED.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_CHANCE_HIGH.get()
        );
        if (!roll(level, chance)) {
            return;
        }
        BlockPos base = player.blockPosition();
        double x = base.getX() + 0.5 + level.random.nextGaussian() * 1.6;
        double y = base.getY() + 1.0 + level.random.nextDouble() * 0.6;
        double z = base.getZ() + 0.5 + level.random.nextGaussian() * 1.6;
        int count = countForIntensity(
                intensity,
                EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_LOW.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_MED.get(),
                EchoRegionsConfig.SCARRED_AMBIENT_PARTICLES_HIGH.get()
        );
        level.sendParticles(ParticleTypes.ASH, x, y, z, count, 0.5, 0.25, 0.5, 0.02);
        double soundChance = chanceForIntensity(
                intensity,
                EchoRegionsConfig.SCARRED_SOUND_CHANCE_LOW.get(),
                EchoRegionsConfig.SCARRED_SOUND_CHANCE_MED.get(),
                EchoRegionsConfig.SCARRED_SOUND_CHANCE_HIGH.get()
        );
        if (roll(level, soundChance)) {
            playAmbientSound(
                    level,
                    base,
                    SoundEvents.STONE_HIT,
                    (float) (double) EchoRegionsConfig.SCARRED_SOUND_VOLUME.get(),
                    (float) (double) EchoRegionsConfig.SCARRED_SOUND_PITCH.get(),
                    cooldowns,
                    gameTime
            );
        }
        cooldowns.lastScarred = gameTime;
    }

    private void triggerHauntedAmbient(
            ServerLevel level,
            ServerPlayer player,
            RegionState.Intensity intensity,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!isNight(level)) {
            return;
        }
        if (!cooldownReady(gameTime, cooldowns.lastHaunted)) {
            return;
        }
        double chance = chanceForIntensity(
                intensity,
                EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_LOW.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_MED.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_CHANCE_HIGH.get()
        );
        if (!roll(level, chance)) {
            return;
        }
        BlockPos base = player.blockPosition();
        double x = base.getX() + 0.5 + level.random.nextGaussian() * 1.5;
        double y = base.getY() + 1.0 + level.random.nextDouble() * 0.6;
        double z = base.getZ() + 0.5 + level.random.nextGaussian() * 1.5;
        int count = countForIntensity(
                intensity,
                EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_LOW.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_MED.get(),
                EchoRegionsConfig.HAUNTED_AMBIENT_PARTICLES_HIGH.get()
        );
        if (count > 0) {
            level.sendParticles(ParticleTypes.SOUL, x, y, z, count, 0.25, 0.25, 0.25, 0.01);
        }
        double soundChance = chanceForIntensity(
                intensity,
                EchoRegionsConfig.HAUNTED_SOUND_CHANCE_LOW.get(),
                EchoRegionsConfig.HAUNTED_SOUND_CHANCE_MED.get(),
                EchoRegionsConfig.HAUNTED_SOUND_CHANCE_HIGH.get()
        );
        if (roll(level, soundChance)) {
            playAmbientSound(
                    level,
                    base,
                    SoundEvents.AMBIENT_CAVE.value(),
                    (float) (double) EchoRegionsConfig.HAUNTED_SOUND_VOLUME.get(),
                    (float) (double) EchoRegionsConfig.HAUNTED_SOUND_PITCH.get(),
                    cooldowns,
                    gameTime
            );
        }
        cooldowns.lastHaunted = gameTime;
    }

    private void triggerWarTornAmbient(
            ServerLevel level,
            ServerPlayer player,
            RegionState.Intensity intensity,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!cooldownReady(gameTime, cooldowns.lastWarTorn)) {
            return;
        }
        double chance = chanceForIntensity(
                intensity,
                EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_LOW.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_MED.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_CHANCE_HIGH.get()
        );
        if (!roll(level, chance)) {
            return;
        }
        BlockPos base = player.blockPosition();
        double x = base.getX() + 0.5 + level.random.nextGaussian() * 1.5;
        double y = base.getY() + 1.0 + level.random.nextDouble() * 0.6;
        double z = base.getZ() + 0.5 + level.random.nextGaussian() * 1.5;
        int count = countForIntensity(
                intensity,
                EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_LOW.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_MED.get(),
                EchoRegionsConfig.WAR_TORN_AMBIENT_PARTICLES_HIGH.get()
        );
        level.sendParticles(ParticleTypes.CRIT, x, y, z, count, 0.35, 0.2, 0.35, 0.04);
        double soundChance = chanceForIntensity(
                intensity,
                EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_LOW.get(),
                EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_MED.get(),
                EchoRegionsConfig.WAR_TORN_SOUND_CHANCE_HIGH.get()
        );
        if (roll(level, soundChance)) {
            playAmbientSound(
                    level,
                    base,
                    SoundEvents.SHIELD_BLOCK.value(),
                    (float) (double) EchoRegionsConfig.WAR_TORN_SOUND_VOLUME.get(),
                    (float) (double) EchoRegionsConfig.WAR_TORN_SOUND_PITCH.get(),
                    cooldowns,
                    gameTime
            );
        }
        cooldowns.lastWarTorn = gameTime;
    }

    private static AggroProfile aggroProfileFor(RegionState state) {
        return switch (state) {
            case HAUNTED -> new AggroProfile(
                    EchoRegionsConfig.HAUNTED_AGGRO_CHANCE.get(),
                    EchoRegionsConfig.HAUNTED_AGGRO_RADIUS.get()
            );
            case WAR_TORN -> new AggroProfile(
                    EchoRegionsConfig.WAR_TORN_AGGRO_CHANCE.get(),
                    EchoRegionsConfig.WAR_TORN_AGGRO_RADIUS.get()
            );
            default -> null;
        };
    }

    private static double intensityMultiplier(RegionState.Intensity intensity) {
        return switch (intensity) {
            case LOW -> 1.0;
            case MED -> 1.15;
            case HIGH -> 1.3;
        };
    }

    private static boolean isAggroCandidate(Monster mob, ServerPlayer player) {
        if (!mob.isAlive()) {
            return false;
        }
        if (mob.getTarget() == player) {
            return false;
        }
        return mob.canAttack(player);
    }

    private static ChatFormatting stateColor(RegionState state) {
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

    private boolean cooldownReady(long gameTime, long lastTick) {
        int cooldown = Math.max(0, EchoRegionsConfig.AMBIENT_COOLDOWN_TICKS.get());
        return cooldown == 0 || gameTime - lastTick >= cooldown;
    }

    private boolean soundCooldownReady(long gameTime, AmbientCooldowns cooldowns) {
        int cooldown = Math.max(0, EchoRegionsConfig.AMBIENT_SOUND_COOLDOWN_TICKS.get());
        return cooldown == 0 || gameTime - cooldowns.lastSound >= cooldown;
    }

    private void playAmbientSound(
            ServerLevel level,
            BlockPos position,
            SoundEvent sound,
            float volume,
            float pitch,
            AmbientCooldowns cooldowns,
            long gameTime
    ) {
        if (!soundCooldownReady(gameTime, cooldowns)) {
            return;
        }
        level.playSound(null, position, sound, SoundSource.AMBIENT, volume, pitch);
        cooldowns.lastSound = gameTime;
    }

    private static boolean roll(ServerLevel level, double chance) {
        return level.random.nextDouble() < chance;
    }

    private static double chanceForIntensity(RegionState.Intensity intensity, double low, double med, double high) {
        return switch (intensity) {
            case LOW -> low;
            case MED -> med;
            case HIGH -> high;
        };
    }

    private static int countForIntensity(RegionState.Intensity intensity, int low, int med, int high) {
        int base = switch (intensity) {
            case LOW -> low;
            case MED -> med;
            case HIGH -> high;
        };
        return capParticleCount(base);
    }

    private static int scaleCount(int base, double multiplier) {
        return capParticleCount((int) Math.round(base * multiplier));
    }

    private static int capParticleCount(int count) {
        int cap = Math.max(0, EchoRegionsConfig.AMBIENT_PARTICLE_BURST_CAP.get());
        int nonNegative = Math.max(0, count);
        return Math.min(nonNegative, cap);
    }

    private static void spawnDust(ServerLevel level, BlockPos pos, int count) {
        if (count <= 0) {
            return;
        }
        level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, count, 0.3, 0.2, 0.3, 0.01);
    }

    private static void spawnBattleDust(ServerLevel level, BlockPos pos, int count) {
        if (count <= 0) {
            return;
        }
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, count, 0.3, 0.2, 0.3, 0.05);
    }

    private static boolean canReceiveScarredBonus(Player player) {
        return player == null || !player.getAbilities().instabuild;
    }

    private static void spawnScarredBonusDrop(ServerLevel level, BlockPos pos) {
        ItemStack bonus = level.random.nextBoolean()
                ? new ItemStack(EchoRegionsItems.PEBBLE.get())
                : new ItemStack(Blocks.COBBLESTONE);
        Block.popResource(level, pos, bonus);
    }
}
