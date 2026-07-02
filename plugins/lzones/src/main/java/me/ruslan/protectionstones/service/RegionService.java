/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package me.ruslan.protectionstones.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.model.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class RegionService {
    private final ProtectionStonesPlugin plugin;
    private final File storageFile;
    private final Map<String, List<Region>> regionsByWorld = new LinkedHashMap<String, List<Region>>();
    private final Map<String, Region> regionsByCore = new LinkedHashMap<String, Region>();

    public RegionService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.storageFile = new File(dataFolder, "regions.yml");
    }

    public void load() throws IOException {
        this.regionsByWorld.clear();
        this.regionsByCore.clear();
        if (!this.storageFile.exists()) {
            return;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration((File)this.storageFile);
        List<Map<?, ?>> raw = configuration.getMapList("regions");
        for (Map<?, ?> entry : raw) {
            try {
                Region region = Region.deserialize(entry);
                this.add(region);
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Skipping broken region entry: " + ex.getMessage());
            }
        }
    }

    public void save() throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        ArrayList<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (Region region : this.allRegions()) {
            serialized.add(region.serialize());
        }
        configuration.set("regions", serialized);
        configuration.save(this.storageFile);
    }

    public Region createRegion(Player player, Location coreLocation, Settings.ProtectionTier shellTier, int bookLevel) {
        World world = coreLocation.getWorld();
        if (world == null) {
            throw new IllegalStateException("Cannot create region in null world");
        }
        Settings settings = this.plugin.getSettings();
        int minY = Math.max(world.getMinHeight(), coreLocation.getBlockY() - settings.heightBelow());
        int maxY = Math.min(world.getMaxHeight(), coreLocation.getBlockY() + settings.heightAbove());
        Region region = new Region(this.allocateRegionName(player), player.getUniqueId(), player.getName(), world.getName(), coreLocation.getBlockX(), coreLocation.getBlockY(), coreLocation.getBlockZ(), settings.radiusForBookLevel(bookLevel), minY, maxY, shellTier.id(), bookLevel, 0);
        this.add(region);
        return region;
    }

    public void add(Region region) {
        this.regionsByWorld.computeIfAbsent(region.worldName(), ignored -> new ArrayList()).add(region);
        this.regionsByCore.put(region.coreKey(), region);
    }

    public boolean remove(Region region) {
        boolean removed = false;
        List<Region> list = this.regionsByWorld.get(region.worldName());
        if (list != null) {
            removed = list.remove(region);
            if (list.isEmpty()) {
                this.regionsByWorld.remove(region.worldName());
            }
        }
        this.regionsByCore.remove(region.coreKey());
        return removed;
    }

    public Optional<Region> regionAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        List<Region> list = this.regionsByWorld.get(location.getWorld().getName());
        if (list == null) {
            return Optional.empty();
        }
        return list.stream().filter(region -> region.contains(location)).findFirst();
    }

    public Optional<Region> regionByCore(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.regionsByCore.get(this.key(location)));
    }

    public List<Region> ownedBy(UUID playerUuid) {
        ArrayList<Region> owned = new ArrayList<Region>();
        for (Region region : this.allRegions()) {
            if (!region.ownerUuid().equals(playerUuid)) continue;
            owned.add(region);
        }
        return owned;
    }

    public Collection<Region> allRegions() {
        ArrayList<Region> all = new ArrayList<Region>();
        for (List<Region> list : this.regionsByWorld.values()) {
            all.addAll(list);
        }
        return all;
    }

    public Collection<Region> allRegionsInWorld(World world) {
        if (world == null) {
            return List.of();
        }
        return List.copyOf(this.regionsByWorld.getOrDefault(world.getName(), List.of()));
    }

    public boolean overlaps(Location center, int radius, int minY, int maxY, Region ignore) {
        if (center == null || center.getWorld() == null) {
            return false;
        }
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        for (Region region : this.regionsByWorld.getOrDefault(center.getWorld().getName(), List.of())) {
            boolean yOverlap;
            if (ignore != null && region.coreKey().equals(ignore.coreKey())) continue;
            boolean xOverlap = maxX >= region.minX() && minX <= region.maxX();
            boolean zOverlap = maxZ >= region.minZ() && minZ <= region.maxZ();
            boolean bl = yOverlap = maxY >= region.minY() && minY <= region.maxY();
            if (!xOverlap || !zOverlap || !yOverlap) continue;
            return true;
        }
        return false;
    }

    public Optional<Region> currentOrNearestOwned(Player player) {
        Optional<Region> current = this.regionAt(player.getLocation());
        if (current.isPresent() && current.orElseThrow().ownerUuid().equals(player.getUniqueId())) {
            return current;
        }
        Region nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Region region : this.ownedBy(player.getUniqueId())) {
            double distance;
            Location core = region.coreLocation();
            if (core.getWorld() == null || !core.getWorld().equals((Object)player.getWorld()) || !((distance = core.distanceSquared(player.getLocation())) < bestDistance)) continue;
            bestDistance = distance;
            nearest = region;
        }
        return Optional.ofNullable(nearest);
    }

    public Optional<Region> ownedRegionByName(Player player, String name) {
        if (player == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        return this.ownedBy(player.getUniqueId()).stream().filter(region -> region.name().equalsIgnoreCase(name)).findFirst();
    }

    public String allocateRegionName(Player player) {
        int counter = this.ownedBy(player.getUniqueId()).size() + 1;
        String base = player.getName();
        String candidate = base + "-" + counter;
        while (this.findByName(candidate).isPresent()) {
            candidate = base + "-" + ++counter;
        }
        return candidate;
    }

    public Optional<Region> findByName(String name) {
        return this.allRegions().stream().filter(region -> region.name().equalsIgnoreCase(name)).findFirst();
    }

    public void upgradeBookLevel(Region region, int newBookLevel) {
        region.setBookLevel(newBookLevel);
        region.setRadius(this.plugin.getSettings().radiusForBookLevel(newBookLevel));
    }

    public boolean damageCore(Region region, int hits) {
        Settings.ProtectionTier shellTier = this.plugin.getSettings().tierById(region.tierId()).orElse(null);
        if (shellTier == null) {
            this.remove(region);
            this.plugin.saveRegionsQuietly();
            return true;
        }
        Location core = region.coreLocation();
        if (core.getWorld() == null) {
            this.remove(region);
            this.plugin.saveRegionsQuietly();
            return true;
        }
        region.setTntDamage(region.tntDamage() + Math.max(1, hits));
        core.getWorld().playSound(core, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.75f);
        this.plugin.getOutlineService().pulseRegion(region);
        for (Player nearby : core.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(core) <= ((double)region.radius() + 12.0) * ((double)region.radius() + 12.0)) {
                this.plugin.getMessageService().actionBar(nearby, "core-damage-" + region.coreKey(), "core-damaged", Map.of("damage", Integer.toString(region.tntDamage()), "max_damage", Integer.toString(shellTier.tntHitsToBreak())));
            }
            if (!region.contains(nearby.getLocation()) || !(this.plugin.getSettings().corePulseDamage() > 0.0)) continue;
            nearby.damage(this.plugin.getSettings().corePulseDamage());
        }
        if (region.tntDamage() >= shellTier.tntHitsToBreak()) {
            core.getBlock().setType(Material.AIR, false);
            this.remove(region);
            this.plugin.saveRegionsQuietly();
            for (Player nearby : core.getWorld().getPlayers()) {
                if (!(nearby.getLocation().distanceSquared(core) <= ((double)region.radius() + 16.0) * ((double)region.radius() + 16.0))) continue;
                this.plugin.getMessageService().actionBar(nearby, "core-destroyed-" + region.coreKey(), "core-destroyed", Map.of());
            }
            core.getWorld().playSound(core, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            return true;
        }
        this.plugin.saveRegionsQuietly();
        return false;
    }

    public Location findHomeLocation(Region region) {
        Location core = region.coreLocation();
        if (core.getWorld() == null) {
            return core;
        }
        World world = core.getWorld();
        int x = core.getBlockX();
        int z = core.getBlockZ();
        int startY = core.getBlockY() + 1;
        int maxY = Math.min(world.getMaxHeight() - 2, core.getBlockY() + this.plugin.getSettings().homeScanHeight());
        for (int y = startY; y <= maxY; ++y) {
            if (!this.isSafeStand(world, x, y, z)) continue;
            return new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5, 0.0f, 0.0f);
        }
        int hardMax = world.getMaxHeight() - 2;
        for (int y = maxY + 1; y <= hardMax; ++y) {
            if (!this.isSafeStand(world, x, y, z)) continue;
            return new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5, 0.0f, 0.0f);
        }
        return new Location(world, (double)x + 0.5, (double)startY, (double)z + 0.5, 0.0f, 0.0f);
    }

    public String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private boolean isSafeStand(World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block floor = world.getBlockAt(x, y - 1, z);
        return feet.isPassable() && head.isPassable() && !floor.isPassable() && floor.getType() != Material.LAVA && floor.getType() != Material.FIRE && floor.getType() != Material.SOUL_FIRE;
    }
}
