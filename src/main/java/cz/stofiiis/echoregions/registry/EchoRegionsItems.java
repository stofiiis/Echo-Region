package cz.stofiiis.echoregions.registry;

import cz.stofiiis.echoregions.EchoRegions;
import cz.stofiiis.echoregions.item.TooltipItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EchoRegionsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EchoRegions.MOD_ID);

    public static final DeferredItem<Item> PEBBLE = ITEMS.registerItem(
            "pebble",
            properties -> new TooltipItem(properties,
                    "item.echoregions.pebble.tooltip",
                    "item.echoregions.pebble.hint")
    );

    public static final DeferredItem<Item> ECTOPLASM = ITEMS.registerItem(
            "ectoplasm",
            properties -> new TooltipItem(properties,
                    "item.echoregions.ectoplasm.tooltip",
                    "item.echoregions.ectoplasm.hint")
    );

    private EchoRegionsItems() {
    }
}
