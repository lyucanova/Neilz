/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package me.ruslan.protectionstones.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.model.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RegionNotificationListener
implements Listener {
    private final ProtectionStonesPlugin plugin;
    private final Map<UUID, String> currentRegionKeys = new HashMap<UUID, String>();

    public RegionNotificationListener(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        Region region = this.plugin.getRegionService().regionAt(event.getTo()).orElse(null);
        String newKey = region == null ? "" : region.coreKey();
        String oldKey = this.currentRegionKeys.getOrDefault(player.getUniqueId(), "");
        if (oldKey.equals(newKey)) {
            return;
        }
        if (!oldKey.isBlank()) {
            this.plugin.getRegionService().allRegions().stream().filter(candidate -> candidate.coreKey().equals(oldKey)).findFirst().ifPresent(previous -> this.plugin.getMessageService().actionBar(player, "exit-zone-" + oldKey, "exit-zone", Map.of("name", previous.name())));
        }
        if (region != null) {
            if (region.ownerUuid().equals(player.getUniqueId())) {
                this.plugin.getMessageService().actionBar(player, "enter-zone-" + newKey, "enter-own", Map.of("name", region.name()));
            } else {
                this.plugin.getMessageService().actionBar(player, "enter-zone-" + newKey, "enter-other", Map.of("owner", region.ownerName()));
            }
            this.currentRegionKeys.put(player.getUniqueId(), newKey);
        } else {
            this.currentRegionKeys.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.currentRegionKeys.remove(event.getPlayer().getUniqueId());
        this.plugin.getMessageService().clearPlayer(event.getPlayer());
    }
}

