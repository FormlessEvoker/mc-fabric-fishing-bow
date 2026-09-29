package com.evoker.fishingbow.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.evoker.fishingbow.FishingBow;

import net.fabricmc.loader.api.FabricLoader;

/** Client-only visual settings for Fishing Bow. */
public final class FishingBowClientConfig {
    private static final String FILE_NAME = "fishing-bow-client.properties";
    private static final float DEFAULT_LINE_WIDTH = 6.0F;
    private static final int DEFAULT_LINE_COLOR = 0xFFD2D2D2;
    private static final float DEFAULT_LINE_SAG = 0.20F;
    private static final float DEFAULT_FIRST_PERSON_LINE_HORIZONTAL_OFFSET = 0.035F;
    private static final float DEFAULT_FIRST_PERSON_LINE_VERTICAL_OFFSET = -0.375F;

    public static float lineWidth = DEFAULT_LINE_WIDTH;
    public static int lineColor = DEFAULT_LINE_COLOR;
    public static float lineSag = DEFAULT_LINE_SAG;
    public static float firstPersonLineHorizontalOffset = DEFAULT_FIRST_PERSON_LINE_HORIZONTAL_OFFSET;
    public static float firstPersonLineVerticalOffset = DEFAULT_FIRST_PERSON_LINE_VERTICAL_OFFSET;

    private FishingBowClientConfig() { }

    public static void initialize() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();
        properties.setProperty("lineWidth", Float.toString(DEFAULT_LINE_WIDTH));
        properties.setProperty("lineColor", String.format("0x%08X", DEFAULT_LINE_COLOR));
        properties.setProperty("lineSag", Float.toString(DEFAULT_LINE_SAG));
        properties.setProperty("firstPersonLineHorizontalOffset",
                Float.toString(DEFAULT_FIRST_PERSON_LINE_HORIZONTAL_OFFSET));
        properties.setProperty("firstPersonLineVerticalOffset",
                Float.toString(DEFAULT_FIRST_PERSON_LINE_VERTICAL_OFFSET));

        try {
            Files.createDirectories(configFile.getParent());
            if (Files.exists(configFile)) {
                // Parse into a separate object so a file that fails partway through leaves every value at its default.
                Properties loaded = new Properties();
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    loaded.load(reader);
                }
                properties.putAll(loaded);
            } else {
                writeDefaults(configFile, properties);
            }
        } catch (IOException | IllegalArgumentException exception) {
            // IllegalArgumentException: Properties.load rejects a malformed Unicode escape.
            FishingBow.LOGGER.warn("Could not load client config {}; using defaults", configFile, exception);
        }

        lineWidth = readFloat(properties, "lineWidth", DEFAULT_LINE_WIDTH, false);
        lineColor = readColor(properties, "lineColor", DEFAULT_LINE_COLOR);
        lineSag = readFloat(properties, "lineSag", DEFAULT_LINE_SAG, true);
        firstPersonLineHorizontalOffset = readSignedFloat(properties, "firstPersonLineHorizontalOffset",
                DEFAULT_FIRST_PERSON_LINE_HORIZONTAL_OFFSET);
        firstPersonLineVerticalOffset = readSignedFloat(properties, "firstPersonLineVerticalOffset",
                DEFAULT_FIRST_PERSON_LINE_VERTICAL_OFFSET);
        FishingBow.LOGGER.info("Fishing Bow client config loaded from {} (lineWidth={}, lineColor=0x{}, "
                        + "lineSag={}, firstPersonLineHorizontalOffset={}, firstPersonLineVerticalOffset={})",
                configFile, lineWidth, String.format("%08X", lineColor), lineSag,
                firstPersonLineHorizontalOffset, firstPersonLineVerticalOffset);
    }

    private static void writeDefaults(Path configFile, Properties properties) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            properties.store(writer, "Fishing Bow client visual settings");
        }
    }

    private static float readFloat(Properties properties, String key, float defaultValue, boolean allowZero) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            float parsed = Float.parseFloat(value.trim());
            if (Float.isFinite(parsed) && (allowZero ? parsed >= 0.0F : parsed > 0.0F)) return parsed;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the same fallback path below.
        }

        FishingBow.LOGGER.warn("Invalid value '{}' for {}; using default {}", value, key, defaultValue);
        return defaultValue;
    }

    private static float readSignedFloat(Properties properties, String key, float defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            float parsed = Float.parseFloat(value.trim());
            if (Float.isFinite(parsed)) return parsed;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the same fallback path below.
        }

        FishingBow.LOGGER.warn("Invalid value '{}' for {}; using default {}", value, key, defaultValue);
        return defaultValue;
    }

    private static int readColor(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        else if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);

        try {
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() == 6 && parsed <= 0xFFFFFFL) return (int) (0xFF000000L | parsed);
            if (hex.length() == 8 && parsed <= 0xFFFFFFFFL) return (int) parsed;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the same fallback path below.
        }

        FishingBow.LOGGER.warn("Invalid color '{}' for {}; using default 0x{}",
                value, key, String.format("%08X", defaultValue));
        return defaultValue;
    }
}
