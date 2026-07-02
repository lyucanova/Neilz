/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package me.ruslan.protectionstones.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.gui.UpgradeMenu;
import me.ruslan.protectionstones.model.Region;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class UpgradeMenuListener
implements Listener {
    private final ProtectionStonesPlugin plugin;
    private final Map<UUID, String> openCoreByPlayer = new HashMap<UUID, String>();

    public UpgradeMenuListener(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Region region) {
        this.openCoreByPlayer.put(player.getUniqueId(), region.coreKey());
        player.openInventory(UpgradeMenu.create(this.plugin, player, region));
        this.plugin.getMessageService().actionBar(player, "upgrade-opened", Map.of());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!"\u00a78\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u0435 \u0437\u043e\u043d\u044b".equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != 13) {
            return;
        }
        String coreKey = this.openCoreByPlayer.get(player.getUniqueId());
        if (coreKey == null) {
            return;
        }
        Region region = this.plugin.getRegionService().allRegions().stream().filter(candidate -> candidate.coreKey().equals(coreKey)).findFirst().orElse(null);
        if (region == null) {
            player.closeInventory();
            return;
        }
        if (!region.ownerUuid().equals(player.getUniqueId()) && !this.plugin.isAdminMode(player)) {
            player.closeInventory();
            return;
        }
        Settings.ProtectionTier nextRadiusTier = this.plugin.getSettings().tierByLevel(region.bookLevel() + 1).orElse(null);
        if (nextRadiusTier == null) {
            this.plugin.getMessageService().send(player, "upgrade-no-next", Map.of());
            player.closeInventory();
            return;
        }
        Location coreLocation = region.coreLocation();
        if (coreLocation.getWorld() == null) {
            player.closeInventory();
            return;
        }
        if (this.plugin.getRegionService().overlaps(coreLocation, nextRadiusTier.radius(), region.minY(), region.maxY(), region)) {
            this.plugin.getMessageService().send(player, "overlap", Map.of());
            player.closeInventory();
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (!this.hasZoneBook((Inventory)inventory, nextRadiusTier.level())) {
            this.plugin.getMessageService().send(player, "upgrade-missing-book", Map.of("book_level", Integer.toString(nextRadiusTier.level())));
            player.closeInventory();
            return;
        }
        this.removeZoneBook((Inventory)inventory, nextRadiusTier.level());
        this.plugin.getRegionService().upgradeBookLevel(region, nextRadiusTier.level());
        this.plugin.saveRegionsQuietly();
        this.plugin.getOutlineService().showRegion(player, region);
        coreLocation.getWorld().playSound(coreLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.1f);
        this.plugin.getMessageService().send(player, "region-upgraded", Map.of("level", this.plugin.getItemService().roman(nextRadiusTier.level()), "radius", Integer.toString(nextRadiusTier.radius())));
        this.openCoreByPlayer.remove(player.getUniqueId());
        player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if ("\u00a78\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u0435 \u0437\u043e\u043d\u044b".equals(event.getView().getTitle())) {
            this.openCoreByPlayer.remove(event.getPlayer().getUniqueId());
        }
    }

    private boolean hasZoneBook(Inventory inventory, int level) {
        for (ItemStack item : inventory.getContents()) {
            OptionalInt bookLevel = this.plugin.getItemService().zoneBookLevel(item);
            if (!bookLevel.isPresent() || bookLevel.getAsInt() != level) continue;
            return true;
        }
        return false;
    }

    private void removeZoneBook(Inventory inventory, int level) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; ++slot) {
            ItemStack item = contents[slot];
            OptionalInt bookLevel = this.plugin.getItemService().zoneBookLevel(item);
            if (!bookLevel.isPresent() || bookLevel.getAsInt() != level) continue;
            if (item.getAmount() <= 1) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - 1);
                inventory.setItem(slot, item);
            }
            return;
        }
    }
}

