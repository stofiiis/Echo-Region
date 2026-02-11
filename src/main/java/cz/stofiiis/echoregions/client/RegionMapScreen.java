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
    private static final String[] LEGEND_STATES = new String[] {
            "scarred",
            "haunted",
            "war_torn",
            "cultivated",
            "settled",
            "blighted",
            "travelled",
            "exploited",
            "neutral"
    };

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

        String dimension = payload.dimensionNamespace() + ":" + payload.dimensionPath();
        String header = "[Echo Regions Map]";
        List<TextLine> headerLines = List.of(
                new TextLine(header, 0xFFFFFFFF),
                new TextLine("Center: " + payload.centerX() + ", " + payload.centerZ(), 0xFFFFFFFF),
                new TextLine("Dimension: " + dimension, 0xFFAAAAAA),
                new TextLine("Radius: " + radius + " (size " + size + "x" + size + ")", 0xFFAAAAAA)
        );
        drawTextBlock(graphics, font, 8, 8, headerLines);

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

        List<TextLine> legendLines = buildLegendLines();
        int legendWidth = maxLineWidth(font, legendLines);
        int legendHeight = legendLines.size() * (font.lineHeight + 2);
        int legendX = startX + gridPixels + 12;
        int legendY = startY;
        boolean placeRight = legendX + legendWidth + 12 <= graphics.guiWidth();
        if (!placeRight) {
            legendX = startX;
            legendY = startY + gridPixels + 12;
            if (legendY + legendHeight + 8 > graphics.guiHeight()) {
                legendY = Math.max(8, graphics.guiHeight() - legendHeight - 12);
            }
        }
        drawTextBlock(graphics, font, legendX, legendY, legendLines);
    }

    private static List<TextLine> buildLegendLines() {
        List<TextLine> lines = new java.util.ArrayList<>();
        lines.add(new TextLine("Legend:", 0xFFFFFFFF));
        for (String id : LEGEND_STATES) {
            String code = ClientStateColors.shortCodeForStateId(id);
            String name = Component.translatable("echoregions.state." + id).getString();
            int color = ClientStateColors.colorForStateId(id);
            lines.add(new TextLine(code + " = " + name, color));
        }
        lines.add(new TextLine("Intensity: L/M/H", 0xFFAAAAAA));
        lines.add(new TextLine("ESC to close", 0xFFAAAAAA));
        return lines;
    }

    private static int maxLineWidth(Font font, List<TextLine> lines) {
        int width = 0;
        for (TextLine line : lines) {
            width = Math.max(width, font.width(line.text()));
        }
        return width;
    }

    private static void drawTextBlock(GuiGraphics graphics, Font font, int x, int y, List<TextLine> lines) {
        int lineHeight = font.lineHeight + 2;
        int width = maxLineWidth(font, lines);
        int height = lines.size() * lineHeight;
        int padding = 4;
        graphics.fill(x - padding, y - padding, x + width + padding, y + height + padding - 2, 0x66000000);
        int cursorY = y;
        for (TextLine line : lines) {
            graphics.drawString(font, line.text(), x, cursorY, line.color(), false);
            cursorY += lineHeight;
        }
    }

    private record TextLine(String text, int color) {
    }
}
