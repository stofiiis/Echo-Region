package cz.stofiiis.echoregions.commands;

import com.mojang.brigadier.CommandDispatcher;

import cz.stofiiis.echoregions.data.RegionMemory;
import cz.stofiiis.echoregions.data.RegionMemoryData;
import cz.stofiiis.echoregions.region.RegionState;
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
                                    RegionState state = RegionState.getState(memory);
                                    RegionState.DominantScore dominant = RegionState.getDominant(memory);
                                    int mining = memory != null ? memory.getMiningScore() : 0;
                                    int combat = memory != null ? memory.getCombatScore() : 0;
                                    int death = memory != null ? memory.getDeathScore() : 0;
                                    long lastUpdated = memory != null ? memory.getLastUpdated() : 0L;

                                    Component message = Component.literal("Chunk " + chunkPos.x + ", " + chunkPos.z + " | state=")
                                            .append(Component.translatable("echoregions.state." + state.getId()))
                                            .append(Component.literal(
                                                    " | dominant=" + dominant.getId()
                                                            + " | thresholdMining=" + RegionState.THRESHOLD_MINING
                                                            + " thresholdCombat=" + RegionState.THRESHOLD_COMBAT
                                                            + " thresholdDeath=" + RegionState.THRESHOLD_DEATH
                                                            + " | mining=" + mining
                                                            + " combat=" + combat
                                                            + " death=" + death
                                                            + " | lastUpdated=" + lastUpdated
                                            ));
                                    source.sendSuccess(() -> message, false);
                                    return 1;
                                }))
        );
    }
}
