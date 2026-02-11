package cz.stofiiis.echoregions.client;

import java.util.ArrayList;
import java.util.List;

import cz.stofiiis.echoregions.network.payload.HudStatePayload;
import cz.stofiiis.echoregions.network.payload.MapDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;

public final class EchoRegionsClient {
    private EchoRegionsClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(EchoRegionsClient::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(EchoRegionsClient::onRenderGui);
    }

    private static void onRegisterPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(HudStatePayload.TYPE, (payload, context) -> ClientDebugData.updateHud(payload));
        event.register(MapDataPayload.TYPE, (payload, context) -> {
            ClientDebugData.updateMap(payload);
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof RegionMapScreen)) {
                minecraft.setScreen(new RegionMapScreen());
            }
        });
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        HudStatePayload payload = ClientDebugData.getHudPayload();
        if (payload == null || !payload.enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics graphics = event.getGuiGraphics();

        String dimension = payload.dimensionNamespace() + ":" + payload.dimensionPath();
        String headlineName = net.minecraft.network.chat.Component
                .translatable("echoregions.state." + payload.headlineId())
                .getString();
        String headlineIntensity = ClientStateColors.intensityLabel(payload.headlineIntensity());
        int headlineColor = ClientStateColors.colorForStateId(payload.headlineId());

        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine("[Echo Regions]", 0x55FFFF));
        lines.add(new HudLine("Region: " + payload.regionX() + ", " + payload.regionZ() + " (" + dimension + ")", 0xFFFFFF));
        lines.add(new HudLine("Headline: " + headlineName + " (" + headlineIntensity + ")", headlineColor));

        if (!payload.tags().isEmpty()) {
            lines.add(new HudLine("Top tags:", 0xFFFFFF));
            for (HudStatePayload.TagSummary tag : payload.tags()) {
                String name = net.minecraft.network.chat.Component
                        .translatable("echoregions.state." + tag.stateId())
                        .getString();
                String intensity = ClientStateColors.intensityLabel(tag.intensity());
                int color = ClientStateColors.colorForStateId(tag.stateId());
                lines.add(new HudLine(" - " + name + " (" + intensity + ")", color));
            }
        } else {
            lines.add(new HudLine("Top tags: none", 0xAAAAAA));
        }
        lines.add(new HudLine("Total weight: " + payload.totalWeight(), 0xFFFFFF));

        int maxWidth = 0;
        for (HudLine line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line.text()));
        }
        int x = graphics.guiWidth() - maxWidth - 6;
        int y = 6;
        int lineHeight = font.lineHeight + 2;
        for (HudLine line : lines) {
            graphics.drawString(font, line.text(), x, y, line.color(), false);
            y += lineHeight;
        }
    }

    private record HudLine(String text, int color) {
    }
}
