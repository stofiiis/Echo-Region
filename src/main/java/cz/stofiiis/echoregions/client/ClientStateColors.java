package cz.stofiiis.echoregions.client;

public final class ClientStateColors {
    private ClientStateColors() {
    }

    public static int colorForStateId(String id) {
        if (id == null) {
            return 0xAAAAAA;
        }
        return switch (id) {
            case "scarred" -> 0xAA0000;
            case "haunted" -> 0xAA00AA;
            case "war_torn" -> 0xFFAA00;
            case "cultivated" -> 0x55FF55;
            case "settled" -> 0x00AA00;
            case "blighted" -> 0xFF5555;
            case "travelled" -> 0x5555FF;
            case "exploited" -> 0x00AAAA;
            case "neutral" -> 0xAAAAAA;
            default -> 0xFFFFFF;
        };
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
