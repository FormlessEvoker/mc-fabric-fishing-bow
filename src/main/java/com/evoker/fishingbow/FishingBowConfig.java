package com.evoker.fishingbow;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Shared gameplay configuration for Fishing Bow. Load before registering the bow item. */
public final class FishingBowConfig {
    private static final String FILE_NAME = "fishing-bow.properties";
    private static final double DEFAULT_ARRIVAL_DISTANCE = 1.25;
    private static final double DEFAULT_PULL_STRENGTH = 0.45;
    private static final double DEFAULT_DRAG = 0.85;
    private static final double DEFAULT_ARROW_BASE_DAMAGE = 1.05;
    private static final double DEFAULT_ITEM_CAPTURE_RADIUS = 1.5;
    private static final float DEFAULT_PROJECTILE_SPEED_MULTIPLIER = 3.0F;
    private static final float DEFAULT_MINIMUM_DRAW_POWER = 0.1F;
    private static final int DEFAULT_BOW_DURABILITY = 64;
    private static final float DEFAULT_MAX_LINE_DISTANCE = 48.0F;
    public static final float LINE_RENDER_MARGIN = 16.0F;

    public static double arrivalDistance = DEFAULT_ARRIVAL_DISTANCE;
    public static double pullStrength = DEFAULT_PULL_STRENGTH;
    public static double drag = DEFAULT_DRAG;
    public static double arrowBaseDamage = DEFAULT_ARROW_BASE_DAMAGE;
    public static double itemCaptureRadius = DEFAULT_ITEM_CAPTURE_RADIUS;
    public static float projectileSpeedMultiplier = DEFAULT_PROJECTILE_SPEED_MULTIPLIER;
    /**
     * Should match between client and server: the client predicts fires with its own value. A lower client value is
     * reconciled by the server, but a mismatch still causes a brief wrong prediction.
     */
    public static float minimumDrawPower = DEFAULT_MINIMUM_DRAW_POWER;
    /** Must match between client and server because the bow item is registered with this value. */
    public static int bowDurability = DEFAULT_BOW_DURABILITY;
    public static float maxLineDistance = DEFAULT_MAX_LINE_DISTANCE;

    private FishingBowConfig() { }

    public static void initialize() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();
        properties.setProperty("arrivalDistance", Double.toString(DEFAULT_ARRIVAL_DISTANCE));
        properties.setProperty("pullStrength", Double.toString(DEFAULT_PULL_STRENGTH));
        properties.setProperty("drag", Double.toString(DEFAULT_DRAG));
        properties.setProperty("arrowBaseDamage", Double.toString(DEFAULT_ARROW_BASE_DAMAGE));
        properties.setProperty("itemCaptureRadius", Double.toString(DEFAULT_ITEM_CAPTURE_RADIUS));
        properties.setProperty("projectileSpeedMultiplier", Float.toString(DEFAULT_PROJECTILE_SPEED_MULTIPLIER));
        properties.setProperty("minimumDrawPower", Float.toString(DEFAULT_MINIMUM_DRAW_POWER));
        properties.setProperty("bowDurability", Integer.toString(DEFAULT_BOW_DURABILITY));
        properties.setProperty("maxLineDistance", Float.toString(DEFAULT_MAX_LINE_DISTANCE));

        try {
            Files.createDirectories(configFile.getParent());
            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            } else {
                writeDefaults(configFile, properties);
            }
        } catch (IOException exception) {
            FishingBow.LOGGER.warn("Could not load config {}; using defaults", configFile, exception);
        }

        arrivalDistance = readDouble(properties, "arrivalDistance", DEFAULT_ARRIVAL_DISTANCE,
                0.000001, Double.MAX_VALUE);
        // Must be positive: startReturning() zeroes the arrow's velocity, so zero pull never brings it back.
        pullStrength = readDouble(properties, "pullStrength", DEFAULT_PULL_STRENGTH,
                0.01, Double.MAX_VALUE);
        drag = readDouble(properties, "drag", DEFAULT_DRAG, 0.0, 1.0);
        arrowBaseDamage = readDouble(properties, "arrowBaseDamage", DEFAULT_ARROW_BASE_DAMAGE,
                0.0, Double.MAX_VALUE);
        itemCaptureRadius = readDouble(properties, "itemCaptureRadius", DEFAULT_ITEM_CAPTURE_RADIUS,
                0.0, Double.MAX_VALUE);
        projectileSpeedMultiplier = readFloat(properties, "projectileSpeedMultiplier",
                DEFAULT_PROJECTILE_SPEED_MULTIPLIER, 0.000001F, Float.MAX_VALUE);
        minimumDrawPower = readFloat(properties, "minimumDrawPower",
                DEFAULT_MINIMUM_DRAW_POWER, 0.0F, 1.0F);
        bowDurability = readInt(properties, "bowDurability", DEFAULT_BOW_DURABILITY,
                1, Integer.MAX_VALUE);
        maxLineDistance = readFloat(properties, "maxLineDistance", DEFAULT_MAX_LINE_DISTANCE,
                4.0F, 96.0F);
        FishingBow.LOGGER.info("Fishing Bow gameplay config loaded from {}", configFile);
    }

    private static void writeDefaults(Path configFile, Properties properties) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            properties.store(writer,
                    "Fishing Bow gameplay configuration; use matching values on client and server");
        }
    }

    private static double readDouble(Properties properties, String key, double defaultValue,
                                     double minimum, double maximum) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            double parsed = Double.parseDouble(value.trim());
            if (Double.isFinite(parsed) && parsed >= minimum && parsed <= maximum) return parsed;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the same fallback path below.
        }

        FishingBow.LOGGER.warn("Invalid value '{}' for {}; using default {}", value, key, defaultValue);
        return defaultValue;
    }

    private static float readFloat(Properties properties, String key, float defaultValue,
                                   float minimum, float maximum) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            float parsed = Float.parseFloat(value.trim());
            if (Float.isFinite(parsed) && parsed >= minimum && parsed <= maximum) return parsed;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the same fallback path below.
        }

        FishingBow.LOGGER.warn("Invalid value '{}' for {}; using default {}", value, key, defaultValue);
        return defaultValue;
    }

    private static int readInt(Properties properties, String key, int defaultValue,
                               int minimum, int maximum) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed >= minimum && parsed <= maximum) return parsed;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the same fallback path below.
        }

        FishingBow.LOGGER.warn("Invalid value '{}' for {}; using default {}", value, key, defaultValue);
        return defaultValue;
    }
}
