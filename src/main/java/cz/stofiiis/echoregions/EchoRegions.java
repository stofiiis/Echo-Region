package cz.stofiiis.echoregions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import cz.stofiiis.echoregions.events.RegionEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(EchoRegions.MOD_ID)
public class EchoRegions {
    public static final String MOD_ID = "echoregions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoRegions(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new RegionEvents());
    }
}
