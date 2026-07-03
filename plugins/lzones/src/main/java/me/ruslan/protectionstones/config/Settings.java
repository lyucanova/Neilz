/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 */
package me.ruslan.protectionstones.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class Settings {
    private final int heightBelow;
    private final int heightAbove;
    private final boolean protectBlocksFromExplosions;
    private final boolean allowPvpInsideZones;
    private final int defaultMaxRegions;
    private final int vipMaxRegions;
    private final int maxMembersPerRegion;
    private final String vipPermission;
    private final int homeScanHeight;
    private final int actionBarCooldownTicks;
    private final double corePulseDamage;
    private final int bombCountdownSeconds;
    private final int bombSpherePoints;
    private final int bombWaveSteps;
    private final int endBombRiseTicks;
    private final double endBombPowerMultiplier;
    private final MegaBombSettings megaBombSettings;
    private final NuclearBombSettings nuclearBombSettings;
    private final LinkedHashMap<Integer, NuclearTierSettings> nuclearTierSettings;
    private final double radiationSuitExposureMultiplier;
    private final OutlineSettings outlineSettings;
    private final LinkedHashMap<String, ProtectionTier> protectionTiers;
    private final LinkedHashMap<Integer, BombTier> bombTiers;
    private final Map<String, String> messages;
    private final List<String> ironCoreRecipeShape;
    private final Map<Character, Material> ironCoreRecipeIngredients;

    private Settings(int heightBelow, int heightAbove, boolean protectBlocksFromExplosions, boolean allowPvpInsideZones, int defaultMaxRegions, int vipMaxRegions, int maxMembersPerRegion, String vipPermission, int homeScanHeight, int actionBarCooldownTicks, double corePulseDamage, int bombCountdownSeconds, int bombSpherePoints, int bombWaveSteps, int endBombRiseTicks, double endBombPowerMultiplier, MegaBombSettings megaBombSettings, NuclearBombSettings nuclearBombSettings, LinkedHashMap<Integer, NuclearTierSettings> nuclearTierSettings, double radiationSuitExposureMultiplier, OutlineSettings outlineSettings, LinkedHashMap<String, ProtectionTier> protectionTiers, LinkedHashMap<Integer, BombTier> bombTiers, Map<String, String> messages, List<String> ironCoreRecipeShape, Map<Character, Material> ironCoreRecipeIngredients) {
        this.heightBelow = heightBelow;
        this.heightAbove = heightAbove;
        this.protectBlocksFromExplosions = protectBlocksFromExplosions;
        this.allowPvpInsideZones = allowPvpInsideZones;
        this.defaultMaxRegions = defaultMaxRegions;
        this.vipMaxRegions = vipMaxRegions;
        this.maxMembersPerRegion = maxMembersPerRegion;
        this.vipPermission = vipPermission;
        this.homeScanHeight = homeScanHeight;
        this.actionBarCooldownTicks = actionBarCooldownTicks;
        this.corePulseDamage = corePulseDamage;
        this.bombCountdownSeconds = bombCountdownSeconds;
        this.bombSpherePoints = bombSpherePoints;
        this.bombWaveSteps = bombWaveSteps;
        this.endBombRiseTicks = endBombRiseTicks;
        this.endBombPowerMultiplier = endBombPowerMultiplier;
        this.megaBombSettings = megaBombSettings;
        this.nuclearBombSettings = nuclearBombSettings;
        this.nuclearTierSettings = nuclearTierSettings;
        this.radiationSuitExposureMultiplier = radiationSuitExposureMultiplier;
        this.outlineSettings = outlineSettings;
        this.protectionTiers = protectionTiers;
        this.bombTiers = bombTiers;
        this.messages = messages;
        this.ironCoreRecipeShape = ironCoreRecipeShape;
        this.ironCoreRecipeIngredients = ironCoreRecipeIngredients;
    }

    public static Settings load(FileConfiguration config) {
        ConfigurationSection general = Settings.section((ConfigurationSection)config, "general");
        ConfigurationSection actionBar = Settings.section(general, "actionbar");
        ConfigurationSection home = Settings.optionalSection(general, "home");
        ConfigurationSection outline = Settings.section(general, "outline");
        ConfigurationSection protection = Settings.section((ConfigurationSection)config, "protection");
        ConfigurationSection tiersSection = Settings.section(protection, "tiers");
        ConfigurationSection explosive = Settings.section((ConfigurationSection)config, "explosive");
        ConfigurationSection explosiveEnd = Settings.optionalSection(explosive, "end");
        ConfigurationSection explosiveMega = Settings.optionalSection(explosive, "mega-five");
        ConfigurationSection explosiveNuclear = Settings.optionalSection(explosive, "nuclear");
        ConfigurationSection explosiveNuclearTiers = Settings.optionalSection(explosive, "nuclear-tiers");
        ConfigurationSection explosiveLevels = Settings.section(explosive, "levels");
        ConfigurationSection messages = Settings.section((ConfigurationSection)config, "messages");
        ConfigurationSection maxRegions = Settings.section(general, "max-regions");
        ConfigurationSection recipe = Settings.section(Settings.section((ConfigurationSection)config, "recipes"), "iron-core");
        MegaBombSettings megaBombSettings = new MegaBombSettings(Math.max(20, explosiveMega.getInt("rise-ticks", 50)), Math.max(10, explosiveMega.getInt("spin-ticks", 35)), Math.max(1.0, explosiveMega.getDouble("rise-height", 5.0)), Math.max(4.0, explosiveMega.getDouble("absorb-radius", 8.0)), Math.max(1, explosiveMega.getInt("absorb-blocks-per-tick", 5)), Math.max(8, explosiveMega.getInt("max-absorbed-blocks", 80)), Math.max(1.5, explosiveMega.getDouble("max-visual-radius", 2.0)), (float)Math.max(4.0, explosiveMega.getDouble("explosion-power", 20.0)), Math.max(8.0, explosiveMega.getDouble("explosion-radius", 28.0)));
        double radiationStartRadius = Math.max(1.0, explosiveNuclear.getDouble("radiation-start-radius", 100.0));
        double radiationMaxRadius = Math.max(radiationStartRadius, explosiveNuclear.getDouble("radiation-radius", 200.0));
        int radiationDurationSeconds = explosiveNuclear.getInt("radiation-duration-seconds", 360);
        NuclearBombSettings nuclearBombSettings = new NuclearBombSettings(Math.max(20, explosiveNuclear.getInt("charge-ticks", 140)), Math.max(8.0, explosiveNuclear.getDouble("blast-radius", 180.0)), Math.max(4.0f, (float)explosiveNuclear.getDouble("explosion-power", 52.0)), Math.max(4, explosiveNuclear.getInt("crater-radius", 38)), Math.max(2, explosiveNuclear.getInt("crater-depth", 18)), Math.max(0, explosiveNuclear.getInt("crater-height", 24)), Math.max(8.0, explosiveNuclear.getDouble("mushroom-height", 170.0)), Math.max(4.0, explosiveNuclear.getDouble("mushroom-radius", 72.0)), radiationStartRadius, radiationMaxRadius, radiationDurationSeconds < 0 ? -1 : Math.max(60, radiationDurationSeconds), Math.max(1, explosiveNuclear.getInt("radiation-expansion-seconds", 2400)), Math.max(0.1, explosiveNuclear.getDouble("exposure-per-second", 1.0)), Math.max(0.0, explosiveNuclear.getDouble("safe-decay-per-second", 0.35)), Math.max(1.0, explosiveNuclear.getDouble("damage-threshold", 12.0)), Math.max(0.1, explosiveNuclear.getDouble("damage-per-second", 1.5)), Math.max(8.0, explosiveNuclear.getDouble("core-shockwave-radius", 180.0)), Math.max(0.1, explosiveNuclear.getDouble("yield-kilotons", 24.0)));
        LinkedHashMap<Integer, NuclearTierSettings> nuclearTiers = new LinkedHashMap<Integer, NuclearTierSettings>();
        for (String key : explosiveNuclearTiers.getKeys(false)) {
            int level = Settings.integer(key, 0);
            if (level < 6) {
                continue;
            }
            nuclearTiers.put(level, Settings.nuclearTierSettings(level, Settings.section(explosiveNuclearTiers, key), nuclearBombSettings));
        }
        OutlineSettings outlineSettings = new OutlineSettings(OutlineMode.fromString(outline.getString("mode", "LINES")), Math.max(20, outline.getInt("duration-ticks", 80)), Math.max(2, outline.getInt("preview-interval-ticks", 6)), Math.max(3, outline.getInt("preview-distance", 6)), Math.max(1, outline.getInt("step", 4)), Math.max(0.4, outline.getDouble("spacing", 0.85)), (float)Math.max(0.5, outline.getDouble("particle-size", 1.1)), Settings.material(outline.getString("fake-block"), Material.LIGHT_BLUE_STAINED_GLASS), Settings.particle(outline.getString("particle"), Particle.END_ROD), Color.fromRGB((int)Settings.clamp(outline.getInt("particle-red", 90)), (int)Settings.clamp(outline.getInt("particle-green", 220)), (int)Settings.clamp(outline.getInt("particle-blue", 255))));
        LinkedHashMap<String, ProtectionTier> tiers = new LinkedHashMap<String, ProtectionTier>();
        ArrayList<ProtectionTier> rawTiers = new ArrayList<ProtectionTier>();
        for (Object key : tiersSection.getKeys(false)) {
            ConfigurationSection tierSection = Settings.section(tiersSection, (String)key);
            ConfigurationSection upgrade = Settings.section(tierSection, "upgrade");
            rawTiers.add(new ProtectionTier((String)key, Math.max(1, tierSection.getInt("level", 1)), Settings.material(tierSection.getString("block"), Material.IRON_BLOCK), Math.max(4, tierSection.getInt("radius", 16)), Math.max(1, tierSection.getInt("tnt-hits", 2)), tierSection.getBoolean("craftable", false), Objects.requireNonNullElse(tierSection.getString("permission"), ""), Objects.requireNonNullElse(upgrade.getString("next"), ""), Math.max(0, upgrade.getInt("book-level", 0)), Settings.parseMaterialMap(Settings.section(upgrade, "ingredients"))));
        }
        rawTiers.stream().sorted(Comparator.comparingInt(ProtectionTier::level)).forEach(tier -> tiers.put(tier.id(), (ProtectionTier)tier));
        LinkedHashMap<Integer, BombTier> bombs = new LinkedHashMap<Integer, BombTier>();
        for (Object key : explosiveLevels.getKeys(false)) {
            int level = Settings.integer((String)key, 1);
            ConfigurationSection bombSection = Settings.section(explosiveLevels, (String)key);
            bombs.put(level, new BombTier(level, Settings.material(bombSection.getString("item"), Material.REDSTONE_BLOCK), (float)bombSection.getDouble("power", 4.0), Math.max(1, bombSection.getInt("core-hits", Math.max(1, level)))));
        }
        LinkedHashMap<String, String> messageMap = new LinkedHashMap<String, String>();
        for (String key : messages.getKeys(false)) {
            messageMap.put(key, Objects.requireNonNullElse(messages.getString(key), ""));
        }
        List<String> recipeShape = recipe.getStringList("shape");
        if (recipeShape.isEmpty()) {
            recipeShape = List.of("III", "ILI", "IEI");
        }
        LinkedHashMap<Character, Material> recipeIngredients = new LinkedHashMap<Character, Material>();
        ConfigurationSection ingredientSection = Settings.section(recipe, "ingredients");
        for (String key : ingredientSection.getKeys(false)) {
            if (key.length() != 1) continue;
            recipeIngredients.put(Character.valueOf(key.charAt(0)), Settings.material(ingredientSection.getString(key), Material.AIR));
        }
        return new Settings(Math.max(1, general.getInt("height-below", 16)), Math.max(1, general.getInt("height-above", 24)), general.getBoolean("protect-blocks-from-explosions", true), general.getBoolean("allow-pvp-inside-zones", false), Math.max(1, maxRegions.getInt("default", 3)), Math.max(1, maxRegions.getInt("vip", 6)), Math.max(1, general.getInt("max-members-per-region", 20)), Objects.requireNonNullElse(maxRegions.getString("vip-permission"), "protectionstones.vip"), Math.max(8, home.getInt("scan-up", 32)), Math.max(1, actionBar.getInt("cooldown-ticks", 25)), Math.max(0.0, general.getDouble("core-pulse-damage", 2.0)), Math.max(1, explosive.getInt("countdown-seconds", 5)), Math.max(8, explosive.getInt("sphere-points", 48)), Math.max(1, explosive.getInt("wave-steps", 6)), Math.max(10, explosiveEnd.getInt("rise-ticks", 40)), Math.max(1.0, explosiveEnd.getDouble("power-multiplier", 2.0)), megaBombSettings, nuclearBombSettings, nuclearTiers, Math.max(0.05, Math.min(1.0, explosiveNuclear.getDouble("radiation-suit-exposure-multiplier", 0.3))), outlineSettings, tiers, bombs, Collections.unmodifiableMap(messageMap), List.copyOf(recipeShape), Map.copyOf(recipeIngredients));
    }

    private static NuclearTierSettings nuclearTierSettings(int level, ConfigurationSection section, NuclearBombSettings fallback) {
        double radiationStartRadius = Math.max(1.0, section.getDouble("radiation-start-radius", fallback.radiationStartRadius()));
        double radiationRadius = Math.max(radiationStartRadius, section.getDouble("radiation-radius", fallback.radiationRadius()));
        return new NuclearTierSettings(level, Objects.requireNonNullElse(section.getString("name"), "Nuclear " + level), Math.max(0.1, section.getDouble("yield-kilotons", fallback.yieldKilotons())), Math.max(8.0, section.getDouble("blast-radius", fallback.blastRadius())), Math.max(4.0f, (float)section.getDouble("explosion-power", fallback.explosionPower())), Math.max(4, section.getInt("crater-radius", fallback.craterRadius())), Math.max(2, section.getInt("crater-depth", fallback.craterDepth())), Math.max(0, section.getInt("crater-height", fallback.craterHeight())), Math.max(8.0, section.getDouble("mushroom-height", fallback.mushroomHeight())), Math.max(4.0, section.getDouble("mushroom-radius", fallback.mushroomRadius())), radiationStartRadius, radiationRadius, Math.max(1, section.getInt("radiation-expansion-seconds", fallback.radiationExpansionSeconds())), Math.max(8.0, section.getDouble("core-shockwave-radius", fallback.coreShockwaveRadius())), section.getBoolean("forbidden", level >= 10), section.getBoolean("world-eater", false), Math.max(0, section.getInt("world-eater-radius", 0)), Math.max(1, section.getInt("world-eater-blocks-per-tick", 650)), Math.max(0, section.getInt("world-eater-max-blocks", 0)));
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("Missing config section: " + path);
        }
        return section;
    }

    private static ConfigurationSection optionalSection(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        return section != null ? section : root.createSection(path);
    }

    private static Material material(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial((String)name.trim().toUpperCase(Locale.ROOT));
        return material != null ? material : fallback;
    }

    private static Particle particle(String name, Particle fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if ("DUST".equals(normalized)) {
            return Particle.REDSTONE;
        }
        try {
            return Particle.valueOf((String)normalized);
        }
        catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static LinkedHashMap<Material, Integer> parseMaterialMap(ConfigurationSection section) {
        LinkedHashMap<Material, Integer> map = new LinkedHashMap<Material, Integer>();
        for (String key : section.getKeys(false)) {
            Material material = Settings.material(key, Material.AIR);
            if (material == Material.AIR) continue;
            int amount = Math.max(1, section.getInt(key, 1));
            map.put(material, amount);
        }
        return map;
    }

    private static int integer(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public int heightBelow() {
        return this.heightBelow;
    }

    public int heightAbove() {
        return this.heightAbove;
    }

    public boolean protectBlocksFromExplosions() {
        return this.protectBlocksFromExplosions;
    }

    public boolean allowPvpInsideZones() {
        return this.allowPvpInsideZones;
    }

    public int actionBarCooldownTicks() {
        return this.actionBarCooldownTicks;
    }

    public int homeScanHeight() {
        return this.homeScanHeight;
    }

    public double corePulseDamage() {
        return this.corePulseDamage;
    }

    public int bombCountdownSeconds() {
        return this.bombCountdownSeconds;
    }

    public int bombSpherePoints() {
        return this.bombSpherePoints;
    }

    public int bombWaveSteps() {
        return this.bombWaveSteps;
    }

    public int endBombRiseTicks() {
        return this.endBombRiseTicks;
    }

    public double endBombPowerMultiplier() {
        return this.endBombPowerMultiplier;
    }

    public MegaBombSettings megaBombSettings() {
        return this.megaBombSettings;
    }

    public NuclearBombSettings nuclearBombSettings() {
        return this.nuclearBombSettings;
    }

    public Optional<NuclearTierSettings> nuclearTier(int level) {
        return Optional.ofNullable(this.nuclearTierSettings.get(level));
    }

    public NuclearTierSettings nuclearTierOrDefault(int level) {
        NuclearTierSettings configured = this.nuclearTierSettings.get(level);
        if (configured != null) {
            return configured;
        }
        return new NuclearTierSettings(level, level >= 10 ? "World Eater" : "Nuclear " + level, this.nuclearBombSettings.yieldKilotons(), this.nuclearBombSettings.blastRadius(), this.nuclearBombSettings.explosionPower(), this.nuclearBombSettings.craterRadius(), this.nuclearBombSettings.craterDepth(), this.nuclearBombSettings.craterHeight(), this.nuclearBombSettings.mushroomHeight(), this.nuclearBombSettings.mushroomRadius(), this.nuclearBombSettings.radiationStartRadius(), this.nuclearBombSettings.radiationRadius(), this.nuclearBombSettings.radiationExpansionSeconds(), this.nuclearBombSettings.coreShockwaveRadius(), level >= 10, false, 0, 650, 0);
    }

    public double radiationSuitExposureMultiplier() {
        return this.radiationSuitExposureMultiplier;
    }

    public OutlineSettings outlineSettings() {
        return this.outlineSettings;
    }

    public List<String> ironCoreRecipeShape() {
        return this.ironCoreRecipeShape;
    }

    public Map<Character, Material> ironCoreRecipeIngredients() {
        return this.ironCoreRecipeIngredients;
    }

    public Map<String, ProtectionTier> protectionTiers() {
        return this.protectionTiers;
    }

    public Map<Integer, BombTier> bombTiers() {
        return this.bombTiers;
    }

    public Optional<ProtectionTier> tierById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.protectionTiers.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<ProtectionTier> tierByLevel(int level) {
        return this.protectionTiers.values().stream().filter(tier -> tier.level() == level).findFirst();
    }

    public Optional<ProtectionTier> tierByBlock(Material material) {
        return this.protectionTiers.values().stream().filter(tier -> tier.blockMaterial() == material).findFirst();
    }

    public Optional<ProtectionTier> nextTier(ProtectionTier tier) {
        if (tier == null || tier.nextTierId() == null || tier.nextTierId().isBlank()) {
            return Optional.empty();
        }
        return this.tierById(tier.nextTierId());
    }

    public Optional<BombTier> bombTier(int level) {
        return Optional.ofNullable(this.bombTiers.get(level));
    }

    public OptionalInt highestProtectionLevel() {
        return this.protectionTiers.values().stream().mapToInt(ProtectionTier::level).max();
    }

    public int radiusForBookLevel(int bookLevel) {
        return this.tierByLevel(bookLevel).map(ProtectionTier::radius).orElseGet(() -> this.protectionTiers.values().stream().min(Comparator.comparingInt(ProtectionTier::level)).map(ProtectionTier::radius).orElse(16));
    }

    public int maxRegionsFor(Player player) {
        if (player != null && !this.vipPermission.isBlank() && player.hasPermission(this.vipPermission)) {
            return this.vipMaxRegions;
        }
        return this.defaultMaxRegions;
    }

    public int maxMembersPerRegion() {
        return this.maxMembersPerRegion;
    }

    public String message(String key) {
        return this.messages.getOrDefault(key, "&cMissing message: " + key);
    }

    public record MegaBombSettings(int riseTicks, int spinTicks, double riseHeight, double absorbRadius, int absorbBlocksPerTick, int maxAbsorbedBlocks, double maxVisualRadius, float explosionPower, double explosionRadius) {
    }

    public record NuclearBombSettings(int chargeTicks, double blastRadius, float explosionPower, int craterRadius, int craterDepth, int craterHeight, double mushroomHeight, double mushroomRadius, double radiationStartRadius, double radiationRadius, int radiationDurationSeconds, int radiationExpansionSeconds, double exposurePerSecond, double safeDecayPerSecond, double damageThreshold, double damagePerSecond, double coreShockwaveRadius, double yieldKilotons) {
    }

    public record NuclearTierSettings(int level, String name, double yieldKilotons, double blastRadius, float explosionPower, int craterRadius, int craterDepth, int craterHeight, double mushroomHeight, double mushroomRadius, double radiationStartRadius, double radiationRadius, int radiationExpansionSeconds, double coreShockwaveRadius, boolean forbidden, boolean worldEater, int worldEaterRadius, int worldEaterBlocksPerTick, int worldEaterMaxBlocks) {
    }

    public record OutlineSettings(OutlineMode mode, int durationTicks, int previewIntervalTicks, int previewDistance, int step, double spacing, float particleSize, Material fakeBlock, Particle particle, Color particleColor) {
    }

    public static enum OutlineMode {
        LINES,
        FAKE_BLOCKS,
        PARTICLES;


        public static OutlineMode fromString(String value) {
            if (value == null || value.isBlank()) {
                return LINES;
            }
            try {
                return OutlineMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored) {
                return LINES;
            }
        }
    }

    public record ProtectionTier(String id, int level, Material blockMaterial, int radius, int tntHitsToBreak, boolean craftable, String permission, String nextTierId, int upgradeBookLevel, LinkedHashMap<Material, Integer> upgradeIngredients) {
        public boolean requiresPermission() {
            return this.permission != null && !this.permission.isBlank();
        }
    }

    public record BombTier(int level, Material itemMaterial, float power, int coreHits) {
    }
}
