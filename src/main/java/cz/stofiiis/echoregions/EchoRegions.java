package cz.stofiiis.echoregions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import cz.stofiiis.echoregions.config.EchoRegionsConfig;
import cz.stofiiis.echoregions.client.EchoRegionsClient;
import cz.stofiiis.echoregions.events.RegionEvents;
import cz.stofiiis.echoregions.network.EchoRegionsNetwork;
import cz.stofiiis.echoregions.registry.EchoRegionsItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;

@Mod(EchoRegions.MOD_ID)
public class EchoRegions {
    public static final String MOD_ID = "echoregions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoRegions(IEventBus modEventBus, ModContainer modContainer) {
        EchoRegionsItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(EchoRegionsNetwork::registerPayloads);
        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            EchoRegionsClient.init(modEventBus);
        }
        modContainer.registerConfig(ModConfig.Type.SERVER, EchoRegionsConfig.SPEC);
        NeoForge.EVENT_BUS.register(new RegionEvents());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(EchoRegionsItems.PEBBLE);
            event.accept(EchoRegionsItems.ECTOPLASM);
        }
    }
}
