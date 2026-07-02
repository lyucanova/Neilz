/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.ruslan.protectionstones;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.ruslan.protectionstones.command.LRCommand;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.listener.CoreListener;
import me.ruslan.protectionstones.listener.RegionNotificationListener;
import me.ruslan.protectionstones.listener.UpgradeMenuListener;
import me.ruslan.protectionstones.model.Region;
import me.ruslan.protectionstones.service.BombService;
import me.ruslan.protectionstones.service.ItemService;
import me.ruslan.protectionstones.service.MessageService;
import me.ruslan.protectionstones.service.OutlineService;
import me.ruslan.protectionstones.service.RecipeService;
import me.ruslan.protectionstones.service.RegionService;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProtectionStonesPlugin
extends JavaPlugin {
    private Settings settings;
    private MessageService messageService;
    private ItemService itemService;
    private RegionService regionService;
    private OutlineService outlineService;
    private BombService bombService;
    private RecipeService recipeService;
    private UpgradeMenuListener upgradeMenuListener;
    private final Set<UUID> adminModePlayers = new HashSet<UUID>();
    private final Set<UUID> visualizationDisabledPlayers = new HashSet<UUID>();

    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadLocalConfig();
        this.messageService = new MessageService(this);
        this.itemService = new ItemService(this);
        this.regionService = new RegionService(this);
        this.outlineService = new OutlineService(this);
        this.bombService = new BombService(this);
        this.recipeService = new RecipeService(this);
        this.upgradeMenuListener = new UpgradeMenuListener(this);
        try {
            this.regionService.load();
        }
        catch (IOException exception) {
            this.getLogger().severe("Failed to load regions.yml: " + exception.getMessage());
        }
        this.recipeService.registerRecipes();
        LRCommand lrCommand = new LRCommand(this);
        if (this.getCommand("lr") != null) {
            this.getCommand("lr").setExecutor((CommandExecutor)lrCommand);
            this.getCommand("lr").setTabCompleter((TabCompleter)lrCommand);
        }
        if (this.getCommand("private") != null) {
            this.getCommand("private").setExecutor((CommandExecutor)lrCommand);
            this.getCommand("private").setTabCompleter((TabCompleter)lrCommand);
        }
        this.getServer().getPluginManager().registerEvents((Listener)new CoreListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.upgradeMenuListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new RegionNotificationListener(this), (Plugin)this);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, this::checkCoreIntegrity, 1200L, 1200L);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> {
            for (Player player : this.getServer().getOnlinePlayers()) {
                this.outlineService.renderPlacementPreview(player);
            }
        }, 10L, Math.max(2L, (long)this.settings.outlineSettings().previewIntervalTicks()));
        this.getLogger().info("LZones " + this.getDescription().getVersion() + " enabled");
    }

    public void onDisable() {
        this.saveRegionsQuietly();
        this.adminModePlayers.clear();
        this.visualizationDisabledPlayers.clear();
    }

    public void reloadLocalConfig() {
        this.reloadConfig();
        if (this.migrateConfigDefaults()) {
            this.saveConfig();
        }
        this.settings = Settings.load(this.getConfig());
    }

    private boolean migrateConfigDefaults() {
        boolean changed = false;
        changed |= this.migrateInt("general.height-below", 16, 32);
        changed |= this.migrateInt("general.height-above", 24, 48);
        changed |= this.setIfMissing("general.max-members-per-region", 20);
        changed |= this.migrateDouble("explosive.mega-five.explosion-power", 28.0, 24.0);
        changed |= this.migrateDouble("explosive.mega-five.explosion-radius", 40.0, 32.0);
        changed |= this.migrateDouble("explosive.levels.1.power", 4.0, 2.5);
        changed |= this.migrateDouble("explosive.levels.2.power", 6.0, 4.0);
        changed |= this.migrateDouble("explosive.levels.3.power", 8.0, 6.0);
        changed |= this.migrateDouble("explosive.levels.4.power", 10.0, 8.0);
        changed |= this.migrateDouble("explosive.levels.5.power", 14.0, 12.0);
        changed |= this.setIfMissing("messages.member-added", "&aИгрок &f{player}&a добавлен в регион &f{region}&a. Участников: &f{count}/{max}");
        changed |= this.setIfMissing("messages.member-already-added", "&eИгрок &f{player}&e уже добавлен в этот регион.");
        changed |= this.setIfMissing("messages.member-owner", "&eВладелец уже имеет полный доступ к региону.");
        changed |= this.setIfMissing("messages.member-limit-reached", "&cВ регионе уже максимум участников: &f{max}");
        changed |= this.setIfMissing("messages.member-removed", "&aИгрок &f{player}&a удалён из региона &f{region}&a.");
        changed |= this.setIfMissing("messages.member-not-found", "&cИгрок &f{player}&c не найден в участниках региона.");
        changed |= this.setIfMissing("messages.member-list-empty", "&eВ регионе &f{region}&e пока нет участников.");
        changed |= this.setIfMissing("messages.member-list", "&7Участники региона &b{region}&7: &f{members} &7(&f{count}/{max}&7)");
        return changed;
    }

    private boolean migrateInt(String path, int oldValue, int newValue) {
        FileConfiguration config = this.getConfig();
        if (!config.isSet(path) || config.getInt(path) == oldValue) {
            config.set(path, newValue);
            return true;
        }
        return false;
    }

    private boolean migrateDouble(String path, double oldValue, double newValue) {
        FileConfiguration config = this.getConfig();
        if (!config.isSet(path) || Double.compare(config.getDouble(path), oldValue) == 0) {
            config.set(path, newValue);
            return true;
        }
        return false;
    }

    private boolean setIfMissing(String path, Object value) {
        FileConfiguration config = this.getConfig();
        if (!config.isSet(path)) {
            config.set(path, value);
            return true;
        }
        return false;
    }

    public void saveRegionsQuietly() {
        try {
            if (this.regionService != null) {
                this.regionService.save();
            }
        }
        catch (IOException exception) {
            this.getLogger().warning("Failed to save regions.yml: " + exception.getMessage());
        }
    }

    private void checkCoreIntegrity() {
        boolean changed = false;
        for (Region region : new ArrayList<Region>(this.regionService.allRegions())) {
            Optional<Settings.ProtectionTier> tierOptional = this.settings.tierById(region.tierId());
            if (tierOptional.isEmpty()) {
                this.regionService.remove(region);
                changed = true;
                continue;
            }
            Location core = region.coreLocation();
            if (core.getWorld() == null) {
                this.regionService.remove(region);
                changed = true;
                continue;
            }
            if (core.getBlock().getType() != tierOptional.orElseThrow().blockMaterial()) {
                this.regionService.remove(region);
                changed = true;
                continue;
            }
            int expectedRadius = this.settings.radiusForBookLevel(region.bookLevel());
            if (region.radius() == expectedRadius) continue;
            region.setRadius(expectedRadius);
            changed = true;
        }
        if (changed) {
            this.saveRegionsQuietly();
        }
    }

    public boolean toggleAdminMode(Player player) {
        if (this.adminModePlayers.contains(player.getUniqueId())) {
            this.adminModePlayers.remove(player.getUniqueId());
            return false;
        }
        this.adminModePlayers.add(player.getUniqueId());
        return true;
    }

    public boolean isAdminMode(Player player) {
        return player != null && this.adminModePlayers.contains(player.getUniqueId());
    }

    public boolean isVisualizationEnabled(Player player) {
        return player != null && !this.visualizationDisabledPlayers.contains(player.getUniqueId());
    }

    public void setVisualizationEnabled(Player player, boolean enabled) {
        if (enabled) {
            this.visualizationDisabledPlayers.remove(player.getUniqueId());
        } else {
            this.visualizationDisabledPlayers.add(player.getUniqueId());
        }
    }

    public Settings getSettings() {
        return this.settings;
    }

    public MessageService getMessageService() {
        return this.messageService;
    }

    public ItemService getItemService() {
        return this.itemService;
    }

    public RegionService getRegionService() {
        return this.regionService;
    }

    public OutlineService getOutlineService() {
        return this.outlineService;
    }

    public BombService getBombService() {
        return this.bombService;
    }

    public UpgradeMenuListener getUpgradeMenuListener() {
        return this.upgradeMenuListener;
    }
}
