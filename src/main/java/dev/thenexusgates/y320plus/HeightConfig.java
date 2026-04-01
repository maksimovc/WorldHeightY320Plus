package dev.thenexusgates.y320plus;

public final class HeightConfig {

    public static final int ENGINE_HEIGHT = 320;
    public static final int ENGINE_HEIGHT_M1 = 319;
    public static final int ENGINE_SECTIONS = 10;
    public static final int SECTION_HEIGHT = 32;
    public static final int BLOCKS_PER_COLUMN = 32 * 32;

    // Engine hard limit: BlockChunk stores height values in ShortBytePalette
    // (getHeight/setHeight/updateHeight all use short). Max signed short = 32767,
    // rounded down to nearest section boundary: floor(32767/32)*32 = 32736.
    private static final int MAX_SAFE_HEIGHT = (Short.MAX_VALUE / SECTION_HEIGHT) * SECTION_HEIGHT;
    private static final int DEFAULT_TARGET = MAX_SAFE_HEIGHT;

    public static final int TARGET_HEIGHT;
    public static final int TARGET_HEIGHT_M1;
    public static final int TARGET_SECTIONS;
    public static final int TARGET_SIZE_BLOCKS_COLUMN;

    static {
        int h = resolveTargetHeight();
        TARGET_HEIGHT = h;
        TARGET_HEIGHT_M1 = h - 1;
        TARGET_SECTIONS = h / SECTION_HEIGHT;
        TARGET_SIZE_BLOCKS_COLUMN = BLOCKS_PER_COLUMN * h;
        System.out.println("[Y320+] World height: " + h + " blocks (" + TARGET_SECTIONS + " sections)");
    }

    private HeightConfig() {
    }

    private static int resolveTargetHeight() {
        String value = firstNonBlank(
            System.getProperty("hytale.worldHeight"),
            System.getProperty("hytale.world.height"),
            System.getenv("HYTALE_WORLD_HEIGHT")
        );
        if (value == null) {
            return DEFAULT_TARGET;
        }
        try {
            return sanitize(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            System.err.println("[Y320+] Invalid height '" + value + "', using " + DEFAULT_TARGET);
            return DEFAULT_TARGET;
        }
    }

    private static int sanitize(int h) {
        if (h <= ENGINE_HEIGHT) {
            System.err.println("[Y320+] Height " + h + " <= " + ENGINE_HEIGHT + ", using " + DEFAULT_TARGET);
            return DEFAULT_TARGET;
        }
        if (h % SECTION_HEIGHT != 0) {
            h = ((h + SECTION_HEIGHT - 1) / SECTION_HEIGHT) * SECTION_HEIGHT;
            System.err.println("[Y320+] Height not multiple of 32, rounded to " + h);
        }
        if (h > MAX_SAFE_HEIGHT) {
            System.err.println("[Y320+] Height " + h + " exceeds max " + MAX_SAFE_HEIGHT + ", clamping");
            return MAX_SAFE_HEIGHT;
        }
        return h;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
