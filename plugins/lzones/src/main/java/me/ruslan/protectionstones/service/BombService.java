/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.FallingBlock
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.entity.EntityChangeBlockEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package me.ruslan.protectionstones.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.model.PendingBomb;
import me.ruslan.protectionstones.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class BombService {
    private final ProtectionStonesPlugin plugin;
    private final Map<String, PendingBomb> pendingBombs = new HashMap<String, PendingBomb>();
    private final Map<UUID, FallingBomb> fallingBombs = new HashMap<UUID, FallingBomb>();

    public BombService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPending(Location location) {
        return this.pendingBombs.containsKey(this.key(location));
    }

    public void arm(Player owner, Location location, Settings.BombTier tier) {
        if (location.getWorld() == null) {
            return;
        }
        if (location.getWorld().getEnvironment() == World.Environment.THE_END) {
            location.getBlock().setType(Material.AIR, false);
            this.launchEndBomb(owner.getUniqueId(), location.clone().add(0.5, 0.5, 0.5), tier);
            return;
        }
        if (this.shouldFall(location)) {
            location.getBlock().setType(Material.AIR, false);
            FallingBlock fallingBlock = location.getWorld().spawnFallingBlock(location.clone().add(0.5, 0.0, 0.5), tier.itemMaterial().createBlockData());
            fallingBlock.setDropItem(false);
            fallingBlock.setHurtEntities(false);
            this.fallingBombs.put(fallingBlock.getUniqueId(), new FallingBomb(owner.getUniqueId(), tier));
            location.getWorld().playSound(location, Sound.ENTITY_TNT_PRIMED, 1.0f, 0.8f);
            return;
        }
        this.startCountdown(new PendingBomb(this.key(location), owner.getUniqueId(), location.clone(), tier.level(), tier.power()));
    }

    public void handleLanding(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) {
            return;
        }
        FallingBlock fallingBlock = (FallingBlock)entity;
        FallingBomb fallingBomb = this.fallingBombs.remove(fallingBlock.getUniqueId());
        if (fallingBomb == null) {
            return;
        }
        Location landed = event.getBlock().getLocation();
        if (!this.canBombOccupy(fallingBomb.ownerUuid(), landed)) {
            event.setCancelled(true);
            fallingBlock.remove();
            Player owner = Bukkit.getPlayer((UUID)fallingBomb.ownerUuid());
            if (owner != null) {
                this.plugin.getMessageService().actionBar(owner, "place-denied", "cannot-place", Map.of());
            }
            return;
        }
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.startCountdown(new PendingBomb(this.key(landed), fallingBomb.ownerUuid(), landed.clone(), fallingBomb.tier().level(), fallingBomb.tier().power())));
    }

    public void remove(Location location) {
        this.pendingBombs.remove(this.key(location));
    }

    private void startCountdown(PendingBomb bomb) {
        final String key = bomb.key();
        if (this.pendingBombs.containsKey(key)) {
            return;
        }
        this.pendingBombs.put(key, bomb);
        Location location = bomb.location();
        World world = location.getWorld();
        Settings.BombTier tier = this.plugin.getSettings().bombTier(bomb.level()).orElse(null);
        if (world != null) {
            world.playSound(location, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
            if (tier != null && tier.level() == 5) {
                world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9f, 0.7f);
            }
        }
        new BukkitRunnable(){
            int secondsLeft;
            {
                this.secondsLeft = BombService.this.plugin.getSettings().bombCountdownSeconds();
            }

            public void run() {
                PendingBomb active = BombService.this.pendingBombs.get(key);
                if (active == null) {
                    this.cancel();
                    return;
                }
                if (this.secondsLeft <= 0) {
                    BombService.this.detonate(active);
                    this.cancel();
                    return;
                }
                BombService.this.animateCountdown(active.location(), this.secondsLeft, active.level());
                --this.secondsLeft;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
    }

    private void detonate(PendingBomb bomb) {
        Settings.BombTier tier;
        this.pendingBombs.remove(bomb.key());
        Location location = bomb.location().clone();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (location.getBlock().getType() != Material.AIR) {
            location.getBlock().setType(Material.AIR, false);
        }
        if ((tier = (Settings.BombTier)this.plugin.getSettings().bombTier(bomb.level()).orElse(null)) != null && tier.level() == 5) {
            this.runMegaSequence(location.clone().add(0.5, 0.5, 0.5), tier, bomb.ownerUuid());
            return;
        }
        this.animateFinalWave(location, Math.max(1.0, (double)bomb.level() * 2.2));
        Player owner = Bukkit.getPlayer((UUID)bomb.ownerUuid());
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.9f);
        Location explosionCenter = location.clone().add(0.5, 0.5, 0.5);
        world.createExplosion(explosionCenter, bomb.power(), false, true, (Entity)owner);
        if (tier != null) {
            this.applyCoreShockwave(explosionCenter, Math.max(4.0, (double)bomb.power() * 1.8), tier.coreHits());
        }
        for (Player nearby : world.getPlayers()) {
            if (!(nearby.getLocation().distanceSquared(location) <= 576.0)) continue;
            this.plugin.getMessageService().actionBar(nearby, "bomb-detonated-" + this.key(location), "bomb-detonated", Map.of());
        }
    }

    private void runMegaSequence(final Location origin, final Settings.BombTier tier, final UUID ownerUuid) {
        final World world = origin.getWorld();
        if (world == null) {
            return;
        }
        final Settings.MegaBombSettings mega = this.plugin.getSettings().megaBombSettings();
        world.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.55f);
        world.playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 0.65f);
        new BukkitRunnable(){
            int tick;
            int absorbedBlocks;
            final List<Item> visuals = new ArrayList<Item>();
            final Location center = origin.clone();

            public void run() {
                if (this.tick <= mega.riseTicks()) {
                    double progress = (double)this.tick / (double)mega.riseTicks();
                    this.center.setY(origin.getY() + mega.riseHeight() * progress);
                    double visualRadius = 0.7 + (mega.maxVisualRadius() - 0.7) * progress;
                    BombService.this.renderMegaCore(this.center, visualRadius, (double)this.tick * 0.18, false);
                    this.absorbedBlocks = BombService.this.absorbNearbyBlocks(this.center, mega, this.absorbedBlocks, this.visuals, true);
                    BombService.this.pullNearbyEntities(this.center, mega.absorbRadius(), 0.09 + progress * 0.08, false);
                    if (this.tick % 8 == 0) {
                        world.playSound(this.center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.8f + (float)progress * 0.5f);
                        world.playSound(this.center, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.45f, 0.7f + (float)progress * 0.3f);
                    }
                    ++this.tick;
                    return;
                }
                int spinTick = this.tick - mega.riseTicks();
                if (spinTick <= mega.spinTicks()) {
                    double spinProgress = (double)spinTick / (double)mega.spinTicks();
                    double pulse = mega.maxVisualRadius() + Math.sin((double)spinTick * 0.45) * 0.35;
                    BombService.this.renderMegaCore(this.center, pulse, (double)this.tick * 0.45, true);
                    this.absorbedBlocks = BombService.this.absorbNearbyBlocks(this.center, mega, this.absorbedBlocks, this.visuals, false);
                    BombService.this.pullNearbyEntities(this.center, mega.absorbRadius() + 6.0, 0.18 + spinProgress * 0.18, true);
                    if (spinTick % 4 == 0) {
                        world.playSound(this.center, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.6f + (float)spinProgress * 0.8f);
                        world.playSound(this.center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.55f, 0.5f + (float)spinProgress * 0.5f);
                    }
                    ++this.tick;
                    return;
                }
                BombService.this.cleanupVisualItems(this.visuals);
                BombService.this.detonateMega(this.center, tier, ownerUuid, mega);
                this.cancel();
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 2L);
    }

    private int absorbNearbyBlocks(Location center, Settings.MegaBombSettings mega, int absorbedBlocks, List<Item> visuals, boolean preferAbove) {
        World world = center.getWorld();
        if (world == null || absorbedBlocks >= mega.maxAbsorbedBlocks()) {
            return absorbedBlocks;
        }
        int absorbedNow = absorbedBlocks;
        List<Block> candidates = this.findBlocksForAbsorption(center, mega.absorbRadius(), preferAbove);
        int limit = Math.min(mega.absorbBlocksPerTick(), mega.maxAbsorbedBlocks() - absorbedNow);
        for (int i = 0; i < candidates.size() && i < limit; ++i) {
            Block block = candidates.get(i);
            Material material = block.getType();
            if (!this.canAbsorb(material) || this.plugin.getRegionService().regionByCore(block.getLocation()).isPresent()) continue;
            Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
            block.setType(Material.AIR, false);
            Item item = world.dropItem(blockCenter, new ItemStack(material));
            item.setCanMobPickup(false);
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setGravity(false);
            item.setUnlimitedLifetime(true);
            Vector velocity = center.toVector().subtract(blockCenter.toVector()).normalize().multiply(0.48).add(new Vector(0.0, 0.16, 0.0));
            item.setVelocity(velocity);
            visuals.add(item);
            world.spawnParticle(Particle.PORTAL, blockCenter, 8, 0.18, 0.18, 0.18, 0.08);
            world.spawnParticle(Particle.SMOKE_LARGE, blockCenter, 5, 0.08, 0.08, 0.08, 0.01);
            ++absorbedNow;
        }
        this.cleanupArrivedItems(center, visuals, mega.maxVisualRadius() + 0.8);
        return absorbedNow;
    }

    private List<Block> findBlocksForAbsorption(Location center, double radius, boolean preferAbove) {
        int z;
        int x;
        int y;
        ArrayList<Block> blocks = new ArrayList<Block>();
        World world = center.getWorld();
        if (world == null) {
            return blocks;
        }
        int blockRadius = Math.max(2, (int)Math.ceil(radius));
        int baseY = center.getBlockY();
        if (preferAbove) {
            for (y = baseY + 1; y <= baseY + 10; ++y) {
                for (x = -blockRadius; x <= blockRadius; ++x) {
                    for (z = -blockRadius; z <= blockRadius; ++z) {
                        this.addAbsorbCandidate(blocks, world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z));
                    }
                }
            }
        }
        for (y = baseY - 6; y <= baseY + 4; ++y) {
            for (x = -blockRadius; x <= blockRadius; ++x) {
                for (z = -blockRadius; z <= blockRadius; ++z) {
                    this.addAbsorbCandidate(blocks, world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z));
                }
            }
        }
        return blocks;
    }

    private void addAbsorbCandidate(List<Block> blocks, Block block) {
        if (block == null || !this.canAbsorb(block.getType())) {
            return;
        }
        blocks.add(block);
    }

    private boolean canAbsorb(Material material) {
        if (material == null || material.isAir()) {
            return false;
        }
        return material.isSolid() && material != Material.BEDROCK && material != Material.BARRIER && material != Material.END_PORTAL_FRAME && material != Material.END_PORTAL && material != Material.NETHER_PORTAL && material != Material.RESPAWN_ANCHOR;
    }

    private void cleanupArrivedItems(Location center, List<Item> visuals, double radius) {
        double maxDistanceSquared = radius * radius;
        visuals.removeIf(item -> {
            if (item == null || !item.isValid()) {
                return true;
            }
            if (item.getLocation().distanceSquared(center) <= maxDistanceSquared) {
                item.remove();
                return true;
            }
            return false;
        });
    }

    private void cleanupVisualItems(List<Item> visuals) {
        for (Item item : visuals) {
            if (item == null || !item.isValid()) continue;
            item.remove();
        }
        visuals.clear();
    }

    private void detonateMega(Location center, Settings.BombTier tier, UUID ownerUuid, Settings.MegaBombSettings mega) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Player owner = Bukkit.getPlayer((UUID)ownerUuid);
        this.renderMegaCore(center, mega.maxVisualRadius() + 1.4, 0.0, true);
        this.animateFinalWave(center, mega.explosionRadius());
        this.animateFinalWave(center, mega.explosionRadius() * 0.7);
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.1f, 0.5f);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.4f, 0.4f);
        world.createExplosion(center, mega.explosionPower(), false, true, (Entity)owner);
        this.applyCoreShockwave(center, mega.explosionRadius(), tier.coreHits());
        for (Entity entity : world.getNearbyEntities(center, mega.explosionRadius(), mega.explosionRadius(), mega.explosionRadius())) {
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity).isDead()) continue;
            double distance = Math.max(1.0, entity.getLocation().distance(center));
            double damage = Math.max(4.0, mega.explosionRadius() - distance * 0.8);
            if (owner != null) {
                living.damage(damage, (Entity)owner);
                continue;
            }
            living.damage(damage);
        }
    }

    private void renderMegaCore(Location center, double radius, double rotation, boolean spin) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB((int)255, (int)30, (int)30), 1.9f);
        Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB((int)120, (int)0, (int)0), 1.4f);
        Particle.DustOptions ember = new Particle.DustOptions(Color.fromRGB((int)255, (int)160, (int)60), 1.2f);
        int points = spin ? 42 : 28;
        for (int i = 0; i < points; ++i) {
            double angle = rotation + Math.PI * 2 / (double)points * (double)i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.REDSTONE, center.clone().add(x, 0.0, z), 1, 0.0, 0.0, 0.0, 0.0, (Object)red);
            world.spawnParticle(Particle.REDSTONE, center.clone().add(x * 0.7, Math.sin(angle * 2.0) * 0.8, z * 0.7), 1, 0.0, 0.0, 0.0, 0.0, (Object)dark);
            world.spawnParticle(Particle.REDSTONE, center.clone().add(x * 0.45, Math.cos(angle * 1.5) * 1.2, z * 0.45), 1, 0.0, 0.0, 0.0, 0.0, (Object)ember);
        }
        for (double y = -1.4; y <= 1.4; y += 0.35) {
            world.spawnParticle(Particle.REDSTONE, center.clone().add(0.0, y, 0.0), 2, radius * 0.18, 0.0, radius * 0.18, 0.0, (Object)red);
        }
        world.spawnParticle(Particle.SMOKE_LARGE, center, 10, radius * 0.5, 0.8, radius * 0.5, 0.02);
        world.spawnParticle(Particle.PORTAL, center, 18, radius * 0.65, 1.0, radius * 0.65, 0.14);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 10, radius * 0.3, 0.6, radius * 0.3, 0.02);
    }

    private void pullNearbyEntities(Location center, double radius, double strength, boolean lift) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity).isDead()) continue;
            Vector delta = center.toVector().subtract(living.getLocation().toVector());
            double distance = Math.max(1.0, delta.length());
            Vector pull = delta.normalize().multiply(strength * Math.max(0.4, 1.0 - distance / radius));
            if (lift) {
                pull.setY(Math.max(pull.getY(), 0.08));
            }
            living.setVelocity(living.getVelocity().multiply(0.72).add(pull));
        }
    }

    private void launchEndBomb(final UUID ownerUuid, final Location origin, final Settings.BombTier tier) {
        final World world = origin.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(origin, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.3f);
        new BukkitRunnable(){
            int tick;

            public void run() {
                if (this.tick >= BombService.this.plugin.getSettings().endBombRiseTicks()) {
                    BombService.this.detonateEndBomb(ownerUuid, origin.clone().add(0.0, 6.0, 0.0), tier);
                    this.cancel();
                    return;
                }
                double progress = (double)this.tick / (double)BombService.this.plugin.getSettings().endBombRiseTicks();
                double y = progress * 6.0;
                double size = 0.9 + progress * ((double)tier.level() * 1.1);
                Location current = origin.clone().add(0.0, y, 0.0);
                BombService.this.animateEndRise(current, size);
                if (this.tick % 8 == 0) {
                    world.playSound(current, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 1.2f);
                }
                ++this.tick;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void detonateEndBomb(UUID ownerUuid, Location location, Settings.BombTier tier) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        double power = (double)tier.power() * this.plugin.getSettings().endBombPowerMultiplier();
        double radius = Math.max(5.0, power * 1.8);
        this.animateFinalWave(location, radius);
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.4f);
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.6f);
        Player owner = Bukkit.getPlayer((UUID)ownerUuid);
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity).isDead()) continue;
            double distance = entity.getLocation().distance(location);
            double damage = Math.max(2.0, power - distance * 0.55);
            if (owner != null) {
                living.damage(damage, (Entity)owner);
                continue;
            }
            living.damage(damage);
        }
        for (Region region : this.plugin.getRegionService().allRegionsInWorld(world)) {
            Location core = region.coreLocation();
            if (!this.coreBlockReachedByBlast(location, core, radius)) continue;
            this.plugin.getRegionService().damageCore(region, Math.max(2, tier.coreHits()));
        }
    }

    private void applyCoreShockwave(Location location, double radius, int coreHits) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (Region region : this.plugin.getRegionService().allRegionsInWorld(world)) {
            Location core = region.coreLocation();
            if (!this.coreBlockReachedByBlast(location, core, radius)) continue;
            this.plugin.getRegionService().damageCore(region, coreHits);
        }
    }

    private boolean coreBlockReachedByBlast(Location blastCenter, Location coreBlock, double radius) {
        if (blastCenter == null || coreBlock == null || blastCenter.getWorld() == null || coreBlock.getWorld() == null || !blastCenter.getWorld().equals((Object)coreBlock.getWorld())) {
            return false;
        }
        double closestX = this.clamp(blastCenter.getX(), coreBlock.getBlockX(), coreBlock.getBlockX() + 1.0);
        double closestY = this.clamp(blastCenter.getY(), coreBlock.getBlockY(), coreBlock.getBlockY() + 1.0);
        double closestZ = this.clamp(blastCenter.getZ(), coreBlock.getBlockZ(), coreBlock.getBlockZ() + 1.0);
        double dx = blastCenter.getX() - closestX;
        double dy = blastCenter.getY() - closestY;
        double dz = blastCenter.getZ() - closestZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean canBombOccupy(UUID ownerUuid, Location location) {
        Optional<Region> region = this.plugin.getRegionService().regionAt(location);
        return region.isEmpty() || region.orElseThrow().canBuild(ownerUuid);
    }

    private boolean shouldFall(Location location) {
        if (location.getWorld() == null || location.getBlockY() <= location.getWorld().getMinHeight()) {
            return false;
        }
        return location.clone().subtract(0.0, 1.0, 0.0).getBlock().isPassable();
    }

    private void animateCountdown(Location location, int secondsLeft, int level) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (Player nearby : world.getPlayers()) {
            if (!(nearby.getLocation().distanceSquared(location) <= 400.0)) continue;
            this.plugin.getMessageService().actionBar(nearby, "bomb-" + this.key(location) + "-" + secondsLeft, "bomb-arming", Map.of("level", this.plugin.getItemService().roman(level), "seconds", Integer.toString(secondsLeft)));
        }
        world.playSound(location, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 0.8f + (float)secondsLeft * 0.05f);
        world.spawnParticle(Particle.REDSTONE, location.clone().add(0.5, 0.9, 0.5), this.plugin.getSettings().bombSpherePoints(), 0.9, 0.9, 0.9, 0.01, (Object)new Particle.DustOptions(Color.RED, 1.5f));
        for (int step = 1; step <= this.plugin.getSettings().bombWaveSteps(); ++step) {
            double radius = (double)step * 0.6;
            for (int i = 0; i < 12; ++i) {
                double angle = 0.5235987755982988 * (double)i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                world.spawnParticle(Particle.REDSTONE, location.clone().add(0.5 + x, 0.2, 0.5 + z), 1, 0.0, 0.0, 0.0, 0.0, (Object)new Particle.DustOptions(Color.fromRGB((int)255, (int)70, (int)70), 1.2f));
            }
        }
    }

    private void animateEndRise(Location location, double size) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB((int)180, (int)90, (int)255), (float)Math.max(1.0, size * 0.35));
        for (int i = 0; i < 26; ++i) {
            double angle = 0.241660973353061 * (double)i;
            double x = Math.cos(angle) * size;
            double z = Math.sin(angle) * size;
            world.spawnParticle(Particle.REDSTONE, location.clone().add(x, 0.0, z), 1, 0.0, 0.0, 0.0, 0.0, (Object)dust);
            world.spawnParticle(Particle.END_ROD, location.clone().add(x * 0.5, 0.2, z * 0.5), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void animateFinalWave(Location location, double maxRadius) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (double radius = 0.8; radius <= maxRadius; radius += 0.8) {
            for (int i = 0; i < 24; ++i) {
                double angle = 0.2617993877991494 * (double)i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                world.spawnParticle(Particle.REDSTONE, location.clone().add(0.5 + x, 0.3, 0.5 + z), 2, 0.05, 0.05, 0.05, 0.01, (Object)new Particle.DustOptions(Color.fromRGB((int)255, (int)30, (int)30), 1.8f));
            }
        }
    }

    private String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record FallingBomb(UUID ownerUuid, Settings.BombTier tier) {
    }
}
