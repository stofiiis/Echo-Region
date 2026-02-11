package cz.stofiiis.echoregions.client;

import java.util.List;

import cz.stofiiis.echoregions.network.payload.MapClosePayload;
import cz.stofiiis.echoregions.network.payload.MapDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class RegionMapScreen extends Screen {
    private static final int CELL_SIZE = 18;

    public RegionMapScreen() {
        super(Component.literal("Echo Regions Map"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientDebugData.clearMap();
        ClientPacketDistributor.sendToServer(new MapClosePayload(true));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x66000000);
        MapDataPayload payload = ClientDebugData.getMapPayload();
        Font font = Minecraft.getInstance().font;
        if (payload == null) {
            graphics.drawString(font, "No map data.", 8, 8, 0xFFFFFFFF, false);
            return;
        }

        int radius = payload.radius();
        int size = radius * 2 + 1;
        int gridPixels = size * CELL_SIZE;
        int startX = (graphics.guiWidth() - gridPixels) / 2;
        int startY = (graphics.guiHeight() - gridPixels) / 2;
        int centerIndex = radius;

        String header = "[Echo Regions Map] radius=" + radius;
        graphics.drawString(font, header, 8, 8, 0xFFFFFFFF, false);

        List<MapDataPayload.Cell> cells = payload.cells();
        int expected = size * size;
        int count = Math.min(expected, cells.size());
        for (int index = 0; index < count; index++) {
            int row = index / size;
            int col = index % size;
            MapDataPayload.Cell cell = cells.get(index);
            int x = startX + col * CELL_SIZE;
            int y = startY + row * CELL_SIZE;

            int bgColor = 0x66000000;
            graphics.fill(x, y, x + CELL_SIZE - 1, y + CELL_SIZE - 1, bgColor);

            if (row == centerIndex && col == centerIndex) {
                int border = 0xFFFFFFFF;
                graphics.fill(x, y, x + CELL_SIZE - 1, y + 1, border);
                graphics.fill(x, y + CELL_SIZE - 2, x + CELL_SIZE - 1, y + CELL_SIZE - 1, border);
                graphics.fill(x, y, x + 1, y + CELL_SIZE - 1, border);
                graphics.fill(x + CELL_SIZE - 2, y, x + CELL_SIZE - 1, y + CELL_SIZE - 1, border);
            }

            String code = ClientStateColors.shortCodeForStateId(cell.stateId());
            char intensity = ClientStateColors.intensityLetter(cell.intensity());
            String label = code + intensity;
            int color = ClientStateColors.colorForStateId(cell.stateId());

            int textWidth = font.width(label);
            int textX = x + (CELL_SIZE - textWidth) / 2;
            int textY = y + (CELL_SIZE - font.lineHeight) / 2;
            graphics.drawString(font, label, textX, textY, color, false);
        }
    }
}
