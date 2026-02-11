package cz.stofiiis.echoregions.client;

public final class ClientStateColors {
    private ClientStateColors() {
    }

    public static int colorForStateId(String id) {
        if (id == null) {
            return argb(0xAAAAAA);
        }
        return switch (id) {
            case "scarred" -> argb(0xAA0000);
            case "haunted" -> argb(0xAA00AA);
            case "war_torn" -> argb(0xFFAA00);
            case "cultivated" -> argb(0x55FF55);
            case "settled" -> argb(0x00AA00);
            case "blighted" -> argb(0xFF5555);
            case "travelled" -> argb(0x5555FF);
            case "exploited" -> argb(0x00AAAA);
            case "neutral" -> argb(0xAAAAAA);
            default -> argb(0xFFFFFF);
        };
    }

    private static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static String shortCodeForStateId(String id) {
        if (id == null) {
            return "??";
        }
        return switch (id) {
            case "scarred" -> "SC";
            case "haunted" -> "HA";
            case "war_torn" -> "WT";
            case "cultivated" -> "CU";
            case "settled" -> "SE";
            case "blighted" -> "BL";
            case "travelled" -> "TR";
            case "exploited" -> "EX";
            case "neutral" -> "NE";
            default -> "??";
        };
    }

    public static String intensityLabel(int intensity) {
        return switch (intensity) {
            case 2 -> "HIGH";
            case 1 -> "MED";
            case 0 -> "LOW";
            default -> "LOW";
        };
    }

    public static char intensityLetter(int intensity) {
        return switch (intensity) {
            case 2 -> 'H';
            case 1 -> 'M';
            default -> 'L';
        };
    }
}
