/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.RayTraceResult
 */
package me.ruslan.protectionstones.service;

import java.util.ArrayList;
import java.util.List;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.model.Region;
import me.ruslan.protectionstones.service.ItemService;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

public final class OutlineService {
    private final ProtectionStonesPlugin plugin;

    public OutlineService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public void showRegion(Player player, Region region) {
        if (player == null || region == null || !this.plugin.isVisualizationEnabled(player)) {
            return;
        }
        Settings.OutlineSettings outline = this.plugin.getSettings().outlineSettings();
        if (outline.mode() == Settings.OutlineMode.FAKE_BLOCKS) {
            List<Location> points = this.collectEdgeLocations(region.coreLocation(), region.radius(), region.minY(), region.maxY(), Math.max(1.0, outline.spacing()));
            this.showFakeBlocks(player, points, outline.fakeBlock().createBlockData(), outline.durationTicks());
            return;
        }
        this.showParticleOutline(player, region.coreLocation(), region.radius(), region.minY(), region.maxY(), outline.particleColor(), outline.durationTicks());
    }

    public void renderPlacementPreview(Player player) {
        if (player == null || !player.isOnline() || !this.plugin.isVisualizationEnabled(player)) {
            return;
        }
        ItemService.ProtectionCoreData coreData = this.heldCore(player);
        if (coreData == null) {
            return;
        }
        Settings settings = this.plugin.getSettings();
        Settings.OutlineSettings outline = settings.outlineSettings();
        RayTraceResult trace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)outline.previewDistance());
        if (trace == null || trace.getHitBlock() == null || trace.getHitBlockFace() == null) {
            return;
        }
        Block target = this.resolvePlacementBlock(trace);
        if (target == null || !target.getType().isAir() && !target.isPassable()) {
            return;
        }
        int radius = this.plugin.getItemService().radiusForCore(coreData);
        int minY = Math.max(player.getWorld().getMinHeight(), target.getY() - settings.heightBelow());
        int maxY = Math.min(player.getWorld().getMaxHeight(), target.getY() + settings.heightAbove());
        boolean valid = this.isValidPreview(player, target.getLocation(), coreData, radius, minY, maxY);
        Color color = valid ? outline.particleColor() : Color.fromRGB((int)255, (int)70, (int)70);
        this.spawnParticleOutline(player, target.getLocation(), radius, minY, maxY, color);
    }

    public void pulseRegion(Region region) {
        if (region == null) {
            return;
        }
        Location core = region.coreLocation();
        World world = core.getWorld();
        if (world == null) {
            return;
        }
        Particle.DustOptions options = new Particle.DustOptions(Color.fromRGB((int)255, (int)80, (int)80), 1.4f);
        List<Location> points = this.collectEdgeLocations(core, region.radius(), region.minY(), region.maxY(), 1.5);
        for (Location point : points) {
            world.spawnParticle(Particle.REDSTONE, point.clone().add(0.5, 0.5, 0.5), 1, 0.0, 0.0, 0.0, 0.0, (Object)options);
        }
        for (double radius = 1.2; radius <= Math.max(3.0, (double)region.radius()); radius += 1.6) {
            for (int i = 0; i < 28; ++i) {
                double angle = 0.2243994752564138 * (double)i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                world.spawnParticle(Particle.REDSTONE, core.clone().add(0.5 + x, 0.4, 0.5 + z), 1, 0.0, 0.0, 0.0, 0.0, (Object)options);
            }
        }
    }

    private void showFakeBlocks(Player player, List<Location> points, BlockData fakeData, int durationTicks) {
        for (Location point : points) {
            player.sendBlockChange(point, fakeData);
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            for (Location point : points) {
                Block block = point.getBlock();
                player.sendBlockChange(point, block.getBlockData());
            }
        }, (long)durationTicks);
    }

    private void showParticleOutline(final Player player, final Location center, final int radius, final int minY, final int maxY, final Color color, final int durationTicks) {
        new BukkitRunnable(){
            int lived;

            public void run() {
                if (!player.isOnline() || this.lived > durationTicks) {
                    this.cancel();
                    return;
                }
                OutlineService.this.spawnParticleOutline(player, center, radius, minY, maxY, color);
                this.lived += 4;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 4L);
    }

    private void spawnParticleOutline(Player player, Location center, int radius, int minY, int maxY, Color color) {
        Settings.OutlineSettings outline = this.plugin.getSettings().outlineSettings();
        Particle particle = outline.particle() == Particle.REDSTONE ? Particle.REDSTONE : outline.particle();
        Particle.DustOptions dust = new Particle.DustOptions(color, outline.particleSize());
        List<Location> points = this.collectEdgeLocations(center, radius, minY, maxY, outline.spacing());
        for (Location point : points) {
            Location at = point.clone().add(0.5, 0.5, 0.5);
            if (particle == Particle.REDSTONE) {
                player.spawnParticle(Particle.REDSTONE, at, 1, (Object)dust);
                continue;
            }
            player.spawnParticle(particle, at, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private List<Location> collectEdgeLocations(Location center, int radius, int minY, int maxY, double spacing) {
        ArrayList<Location> points = new ArrayList<Location>();
        if (center == null || center.getWorld() == null) {
            return points;
        }
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        double safeSpacing = Math.max(0.4, spacing);
        for (double x = (double)minX; x <= (double)maxX + 0.001; x += safeSpacing) {
            this.add(points, center, x, minY, minZ);
            this.add(points, center, x, minY, maxZ);
            this.add(points, center, x, maxY, minZ);
            this.add(points, center, x, maxY, maxZ);
        }
        for (double z = (double)minZ; z <= (double)maxZ + 0.001; z += safeSpacing) {
            this.add(points, center, minX, minY, z);
            this.add(points, center, maxX, minY, z);
            this.add(points, center, minX, maxY, z);
            this.add(points, center, maxX, maxY, z);
        }
        for (double y = (double)minY; y <= (double)maxY + 0.001; y += safeSpacing) {
            this.add(points, center, minX, y, minZ);
            this.add(points, center, maxX, y, minZ);
            this.add(points, center, minX, y, maxZ);
            this.add(points, center, maxX, y, maxZ);
        }
        return points;
    }

    private boolean isValidPreview(Player player, Location location, ItemService.ProtectionCoreData coreData, int radius, int minY, int maxY) {
        if (coreData.shellTier().requiresPermission() && !player.hasPermission(coreData.shellTier().permission())) {
            return false;
        }
        if (this.plugin.getRegionService().ownedBy(player.getUniqueId()).size() >= this.plugin.getSettings().maxRegionsFor(player)) {
            return false;
        }
        return !this.plugin.getRegionService().overlaps(location, radius, minY, maxY, null);
    }

    private ItemService.ProtectionCoreData heldCore(Player player) {
        return this.plugin.getItemService().protectionCore(player.getInventory().getItemInMainHand()).orElseGet(() -> this.plugin.getItemService().protectionCore(player.getInventory().getItemInOffHand()).orElse(null));
    }

    private Block resolvePlacementBlock(RayTraceResult trace) {
        Block hitBlock = trace.getHitBlock();
        BlockFace face = trace.getHitBlockFace();
        if (hitBlock == null || face == null) {
            return null;
        }
        return hitBlock.getRelative(face);
    }

    private void add(List<Location> points, Location template, double x, double y, double z) {
        World world = template.getWorld();
        if (world == null) {
            return;
        }
        points.add(new Location(world, x, y, z));
    }
}

