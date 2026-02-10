package cz.stofiiis.echoregions.commands;

import com.mojang.brigadier.CommandDispatcher;

import cz.stofiiis.echoregions.data.RegionMemory;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import cz.stofiiis.echoregions.region.RegionState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class EchoRegionCommand {
    private EchoRegionCommand() {
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
                                    RegionMemoryData data = RegionMemoryData.get(level);
                                    RegionMemory memory = data.getMemory(chunkPos);
                                    RegionState.AggregatedScores aggregated = RegionState.aggregateScores(data, chunkPos);
                                    RegionState state = RegionState.getState(aggregated);
                                    RegionState.DominantScore dominant = RegionState.getDominant(aggregated);
                                    int mining = memory != null ? memory.getMiningScore() : 0;
                                    int combat = memory != null ? memory.getCombatScore() : 0;
                                    int death = memory != null ? memory.getDeathScore() : 0;
                                    int farm = memory != null ? memory.getFarmScore() : 0;
                                    int build = memory != null ? memory.getBuildScore() : 0;
                                    int fire = memory != null ? memory.getFireScore() : 0;
                                    int travel = memory != null ? memory.getTravelScore() : 0;
                                    int exploit = memory != null ? memory.getExploitScore() : 0;
                                    long lastUpdated = memory != null ? memory.getLastUpdated() : 0L;
                                    long ticksAgo = lastUpdated > 0 ? Math.max(0, level.getGameTime() - lastUpdated) : 0L;
                                    String dimension = level.dimension().identifier().toString();

                                    ChatFormatting stateColor = switch (state) {
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

                                    Component header = Component.literal("[EchoRegions]").withStyle(ChatFormatting.AQUA);
                                    Component stateComponent = Component.translatable("echoregions.state." + state.getId()).withStyle(stateColor);

                                    Component message = Component.empty()
                                            .append(header)
                                            .append(Component.literal("\n> Chunk: " + chunkPos.x + ", " + chunkPos.z))
                                            .append(Component.literal("\n> Dimension: " + dimension))
                                            .append(Component.literal("\n> State: "))
                                            .append(stateComponent)
                                            .append(Component.literal(" (dominant=" + dominant.getId() + ")"))
                                            .append(Component.literal("\n> Thresholds:"))
                                            .append(Component.literal("\n  - mining=" + RegionState.getThresholdMining()))
                                            .append(Component.literal("\n  - combat=" + RegionState.getThresholdCombat()))
                                            .append(Component.literal("\n  - death=" + RegionState.getThresholdDeath()))
                                            .append(Component.literal("\n  - farm=" + RegionState.getThresholdFarm()))
                                            .append(Component.literal("\n  - build=" + RegionState.getThresholdBuild()))
                                            .append(Component.literal("\n  - fire=" + RegionState.getThresholdFire()))
                                            .append(Component.literal("\n  - travel=" + RegionState.getThresholdTravel()))
                                            .append(Component.literal("\n  - exploit=" + RegionState.getThresholdExploit()))
                                            .append(Component.literal("\n\nLocal (chunk):"))
                                            .append(Component.literal("\n  - mining: " + mining))
                                            .append(Component.literal("\n  - combat: " + combat))
                                            .append(Component.literal("\n  - death: " + death))
                                            .append(Component.literal("\n  - farm: " + farm))
                                            .append(Component.literal("\n  - build: " + build))
                                            .append(Component.literal("\n  - fire: " + fire))
                                            .append(Component.literal("\n  - travel: " + travel))
                                            .append(Component.literal("\n  - exploit: " + exploit))
                                            .append(Component.literal("\n\nRegion (3x3):"))
                                            .append(Component.literal("\n  - mining: " + aggregated.mining()))
                                            .append(Component.literal("\n  - combat: " + aggregated.combat()))
                                            .append(Component.literal("\n  - death: " + aggregated.death()))
                                            .append(Component.literal("\n  - farm: " + aggregated.farm()))
                                            .append(Component.literal("\n  - build: " + aggregated.build()))
                                            .append(Component.literal("\n  - fire: " + aggregated.fire()))
                                            .append(Component.literal("\n  - travel: " + aggregated.travel()))
                                            .append(Component.literal("\n  - exploit: " + aggregated.exploit()))
                                            .append(Component.literal("\n\nDecay:"))
                                            .append(Component.literal("\n  - lastUpdated: " + lastUpdated + " (ago " + ticksAgo + " ticks)"));
                                    source.sendSuccess(() -> message, false);
                                    return 1;
                                }))
        );
    }
}
