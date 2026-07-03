/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockDamageEvent
 *  org.bukkit.event.block.BlockExplodeEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityChangeBlockEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityExplodeEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 */
package me.ruslan.protectionstones.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.model.Region;
import me.ruslan.protectionstones.service.ItemService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class CoreListener
implements Listener {
    private final ProtectionStonesPlugin plugin;

    public CoreListener(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        Optional<Region> region;
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block block = event.getBlockPlaced();
        Optional<ItemService.ProtectionCoreData> coreData = this.plugin.getItemService().protectionCore(item);
        if (coreData.isPresent()) {
            this.handleProtectionCorePlacement(event, player, block.getLocation(), coreData.orElseThrow());
            return;
        }
        Optional<Settings.BombTier> bombTier = this.plugin.getItemService().bombTier(item);
        if (bombTier.isPresent()) {
            Optional<Region> bombRegion = this.plugin.getRegionService().regionAt(block.getLocation());
            if (bombRegion.isPresent() && !this.canBuild(player, bombRegion.orElseThrow())) {
                event.setCancelled(true);
                this.plugin.getMessageService().actionBar(player, "place-denied", "cannot-place", Map.of());
                return;
            }
            Settings.BombTier tier = bombTier.orElseThrow();
            if (!this.plugin.getBombService().confirmForbiddenBomb(player, tier)) {
                event.setCancelled(true);
                return;
            }
            this.plugin.getBombService().arm(player, block.getLocation(), tier);
            return;
        }
        if (this.plugin.getItemService().looksLikeCoreMaterial(item)) {
            this.plugin.getMessageService().actionBar(player, "normal-block-core", "normal-block", Map.of());
        }
        if ((region = this.plugin.getRegionService().regionAt(block.getLocation())).isPresent() && !this.canBuild(player, region.orElseThrow())) {
            event.setCancelled(true);
            this.plugin.getMessageService().actionBar(player, "place-denied", "cannot-place", Map.of());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFallingBombLand(EntityChangeBlockEvent event) {
        this.plugin.getBombService().handleLanding(event);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(BlockDamageEvent event) {
        Optional<Region> coreRegion = this.plugin.getRegionService().regionByCore(event.getBlock().getLocation());
        if (coreRegion.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.canManage(player, coreRegion.orElseThrow())) {
            return;
        }
        if (this.canForceCoreBreak(player)) {
            event.setInstaBreak(true);
        }
    }

    private void handleProtectionCorePlacement(BlockPlaceEvent event, Player player, Location location, ItemService.ProtectionCoreData coreData) {
        int max;
        Settings.ProtectionTier shellTier = coreData.shellTier();
        int radius = this.plugin.getItemService().radiusForCore(coreData);
        if (shellTier.requiresPermission() && !player.hasPermission(shellTier.permission())) {
            event.setCancelled(true);
            this.plugin.getMessageService().send(player, "need-permission-tier", Map.of("permission", shellTier.permission()));
            return;
        }
        int owned = this.plugin.getRegionService().ownedBy(player.getUniqueId()).size();
        if (owned >= (max = this.plugin.getSettings().maxRegionsFor(player))) {
            event.setCancelled(true);
            this.plugin.getMessageService().send(player, "limit-reached", Map.of("limit", Integer.toString(max)));
            return;
        }
        int minY = Math.max(player.getWorld().getMinHeight(), location.getBlockY() - this.plugin.getSettings().heightBelow());
        int maxY = Math.min(player.getWorld().getMaxHeight(), location.getBlockY() + this.plugin.getSettings().heightAbove());
        if (this.plugin.getRegionService().overlaps(location, radius, minY, maxY, null)) {
            event.setCancelled(true);
            this.plugin.getMessageService().send(player, "overlap", Map.of());
            return;
        }
        Region region = this.plugin.getRegionService().createRegion(player, location, shellTier, coreData.bookLevel());
        this.plugin.saveRegionsQuietly();
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.15f);
        this.plugin.getMessageService().send(player, "region-created", Map.of("radius", Integer.toString(radius)));
        this.plugin.getOutlineService().showRegion(player, region);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (this.plugin.getBombService().isPending(location)) {
            event.setCancelled(true);
            return;
        }
        Optional<Region> coreRegion = this.plugin.getRegionService().regionByCore(location);
        if (coreRegion.isPresent()) {
            Region region = coreRegion.orElseThrow();
            if (!this.canManage(player, region)) {
                event.setCancelled(true);
                this.plugin.getMessageService().actionBar(player, "break-core-denied", "cannot-break", Map.of());
                return;
            }
            event.setDropItems(false);
            Settings.ProtectionTier shellTier = this.plugin.getSettings().tierById(region.tierId()).orElse(null);
            if (shellTier != null) {
                location.getWorld().dropItemNaturally(location, this.plugin.getItemService().createProtectionCore(shellTier, region.bookLevel()));
            }
            this.plugin.getRegionService().remove(region);
            this.plugin.saveRegionsQuietly();
            this.plugin.getMessageService().send(player, "region-removed", Map.of());
            return;
        }
        Optional<Region> containing = this.plugin.getRegionService().regionAt(location);
        if (containing.isPresent() && !this.canBuild(player, containing.orElseThrow())) {
            event.setCancelled(true);
            this.plugin.getMessageService().actionBar(player, "break-denied", "cannot-break", Map.of());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event) {
        boolean interactable;
        if (event.getClickedBlock() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        Location location = clicked.getLocation();
        Optional<Region> coreRegion = this.plugin.getRegionService().regionByCore(location);
        if (player.isSneaking() && coreRegion.isPresent()) {
            Region region = coreRegion.orElseThrow();
            if (this.canManage(player, region)) {
                event.setCancelled(true);
                this.plugin.getUpgradeMenuListener().open(player, region);
            }
            return;
        }
        Optional<Region> region = this.plugin.getRegionService().regionAt(location);
        if (region.isEmpty() || this.canBuild(player, region.orElseThrow())) {
            return;
        }
        boolean bl = interactable = clicked.getState() instanceof InventoryHolder || clicked.getType().isInteractable();
        if (interactable) {
            event.setCancelled(true);
            this.plugin.getMessageService().actionBar(player, "interact-denied", "cannot-interact", Map.of());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPvp(EntityDamageByEntityEvent event) {
        boolean insideZone;
        Player attacker;
        Entity victim;
        block6: {
            block5: {
                if (this.plugin.getSettings().allowPvpInsideZones()) {
                    return;
                }
                Entity damager = event.getDamager();
                victim = event.getEntity();
                if (!(damager instanceof Player)) break block5;
                attacker = (Player)damager;
                if (victim instanceof Player) break block6;
            }
            return;
        }
        Player target = (Player)victim;
        boolean bl = insideZone = this.plugin.getRegionService().regionAt(attacker.getLocation()).isPresent() || this.plugin.getRegionService().regionAt(target.getLocation()).isPresent();
        if (!insideZone) {
            return;
        }
        event.setCancelled(true);
        this.plugin.getMessageService().actionBar(attacker, "pvp-denied", "pvp-denied", Map.of());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent event) {
        this.processExplosion(event.blockList());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockExplode(BlockExplodeEvent event) {
        this.processExplosion(event.blockList());
    }

    private void processExplosion(List<Block> affectedBlocks) {
        ArrayList<Block> snapshot = new ArrayList<Block>(affectedBlocks);
        for (Block block : snapshot) {
            Optional<Region> coreRegion = this.plugin.getRegionService().regionByCore(block.getLocation());
            if (coreRegion.isPresent()) {
                affectedBlocks.remove(block);
                this.plugin.getRegionService().damageCore(coreRegion.orElseThrow(), 1);
                continue;
            }
            if (!this.plugin.getSettings().protectBlocksFromExplosions() || !this.plugin.getRegionService().regionAt(block.getLocation()).isPresent()) continue;
            affectedBlocks.remove(block);
        }
    }

    private boolean canBuild(Player player, Region region) {
        return player != null && region.canBuild(player.getUniqueId()) || this.plugin.isAdminMode(player);
    }

    private boolean canManage(Player player, Region region) {
        return player != null && region.isOwner(player.getUniqueId()) || this.plugin.isAdminMode(player);
    }

    private boolean canForceCoreBreak(Player player) {
        if (player == null) {
            return false;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        Material tool = player.getInventory().getItemInMainHand().getType();
        return tool.name().endsWith("_PICKAXE");
    }
}
