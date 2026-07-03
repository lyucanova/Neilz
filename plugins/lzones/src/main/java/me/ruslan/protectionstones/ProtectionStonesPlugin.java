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
import me.ruslan.protectionstones.command.TntShopCommand;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.listener.CoreListener;
import me.ruslan.protectionstones.listener.RegionNotificationListener;
import me.ruslan.protectionstones.listener.UpgradeMenuListener;
import me.ruslan.protectionstones.model.Region;
import me.ruslan.protectionstones.service.BombService;
import me.ruslan.protectionstones.service.EconomyService;
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
    private EconomyService economyService;
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
        this.economyService = new EconomyService(this);
        this.recipeService = new RecipeService(this);
        this.upgradeMenuListener = new UpgradeMenuListener(this);
        try {
            this.regionService.load();
        }
        catch (IOException exception) {
            this.getLogger().severe("Failed to load regions.yml: " + exception.getMessage());
        }
        try {
            this.bombService.loadRadiationZones();
        }
        catch (IOException exception) {
            this.getLogger().warning("Failed to load radiation.yml: " + exception.getMessage());
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
        TntShopCommand tntShopCommand = new TntShopCommand(this);
        if (this.getCommand("tntshop") != null) {
            this.getCommand("tntshop").setExecutor((CommandExecutor)tntShopCommand);
            this.getCommand("tntshop").setTabCompleter((TabCompleter)tntShopCommand);
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
        this.getServer().getScheduler().runTaskTimer((Plugin)this, this.bombService::tickRadiation, 20L, 20L);
        this.getLogger().info("LZones " + this.getDescription().getVersion() + " enabled");
    }

    public void onDisable() {
        this.saveRegionsQuietly();
        if (this.bombService != null) {
            try {
                this.bombService.saveRadiationZones();
            }
            catch (IOException exception) {
                this.getLogger().warning("Failed to save radiation.yml: " + exception.getMessage());
            }
        }
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
        changed |= this.setIfMissing("shop.tnt-prices.1", 20000.0);
        changed |= this.setIfMissing("shop.tnt-prices.2", 250000.0);
        changed |= this.setIfMissing("shop.tnt-prices.3", 650000.0);
        changed |= this.setIfMissing("shop.tnt-prices.4", 1000000.0);
        changed |= this.setIfMissing("shop.tnt-prices.5", 15000000.0);
        changed |= this.setIfMissing("explosive.levels.6.item", "TNT");
        changed |= this.migrateDouble("explosive.levels.6.power", 18.0, 24.0);
        changed |= this.migrateDouble("explosive.levels.6.power", 30.0, 24.0);
        changed |= this.migrateInt("explosive.levels.6.core-hits", 14, 20);
        changed |= this.setBombLevelDefaults(7, "TNT", 15000.0, 40);
        changed |= this.setBombLevelDefaults(8, "TNT", 50000.0, 80);
        changed |= this.setBombLevelDefaults(9, "TNT", 100000.0, 160);
        changed |= this.setBombLevelDefaults(10, "TNT", 1000000.0, 999);
        changed |= this.migrateInt("explosive.nuclear.charge-ticks", 100, 140);
        changed |= this.migrateInt("explosive.nuclear.charge-ticks", 120, 140);
        changed |= this.migrateDouble("explosive.nuclear.blast-radius", 30.0, 180.0);
        changed |= this.migrateDouble("explosive.nuclear.blast-radius", 100.0, 180.0);
        changed |= this.migrateDouble("explosive.nuclear.blast-radius", 120.0, 180.0);
        changed |= this.migrateDouble("explosive.nuclear.explosion-power", 18.0, 52.0);
        changed |= this.migrateDouble("explosive.nuclear.explosion-power", 22.0, 52.0);
        changed |= this.migrateDouble("explosive.nuclear.explosion-power", 36.0, 52.0);
        changed |= this.migrateInt("explosive.nuclear.crater-radius", 10, 30);
        changed |= this.migrateInt("explosive.nuclear.crater-radius", 24, 30);
        changed |= this.migrateInt("explosive.nuclear.crater-radius", 30, 38);
        changed |= this.migrateInt("explosive.nuclear.crater-depth", 5, 14);
        changed |= this.migrateInt("explosive.nuclear.crater-depth", 8, 14);
        changed |= this.migrateInt("explosive.nuclear.crater-depth", 14, 18);
        changed |= this.migrateInt("explosive.nuclear.crater-height", 18, 24);
        changed |= this.migrateDouble("explosive.nuclear.mushroom-height", 36.0, 170.0);
        changed |= this.migrateDouble("explosive.nuclear.mushroom-height", 95.0, 170.0);
        changed |= this.migrateDouble("explosive.nuclear.mushroom-height", 120.0, 170.0);
        changed |= this.migrateDouble("explosive.nuclear.mushroom-radius", 13.0, 72.0);
        changed |= this.migrateDouble("explosive.nuclear.mushroom-radius", 38.0, 72.0);
        changed |= this.migrateDouble("explosive.nuclear.mushroom-radius", 52.0, 72.0);
        changed |= this.setIfMissing("explosive.nuclear.radiation-start-radius", 100.0);
        changed |= this.migrateDouble("explosive.nuclear.radiation-radius", 28.0, 200.0);
        changed |= this.migrateDouble("explosive.nuclear.radiation-radius", 300.0, 200.0);
        changed |= this.migrateInt("explosive.nuclear.radiation-duration-seconds", 180, 360);
        changed |= this.migrateInt("explosive.nuclear.radiation-duration-seconds", 240, 360);
        changed |= this.migrateInt("explosive.nuclear.radiation-duration-seconds", -1, 360);
        changed |= this.setIfMissing("explosive.nuclear.radiation-expansion-seconds", 2400);
        changed |= this.setIfMissing("explosive.nuclear.exposure-per-second", 1.0);
        changed |= this.setIfMissing("explosive.nuclear.radiation-suit-exposure-multiplier", 0.30);
        changed |= this.setIfMissing("explosive.nuclear.radiation-suit-exposure-per-10-seconds", 0.5);
        changed |= this.setIfMissing("explosive.nuclear.safe-decay-per-second", 0.35);
        changed |= this.setIfMissing("explosive.nuclear.damage-threshold", 12.0);
        changed |= this.setIfMissing("explosive.nuclear.damage-per-second", 1.5);
        changed |= this.migrateDouble("explosive.nuclear.core-shockwave-radius", 34.0, 180.0);
        changed |= this.migrateDouble("explosive.nuclear.core-shockwave-radius", 100.0, 180.0);
        changed |= this.migrateDouble("explosive.nuclear.core-shockwave-radius", 120.0, 180.0);
        changed |= this.setIfMissing("explosive.nuclear.yield-kilotons", 24.0);
        changed |= this.setNuclearTierDefaults(6, "Fat Man+", 24.0, 180.0, 52.0, 38, 18, 24, 170.0, 72.0, 100.0, 200.0, 2400, 180.0, false, false, 0, 650, 0);
        changed |= this.setNuclearTierDefaults(7, "Castle Bravo", 15000.0, 240.0, 68.0, 48, 24, 30, 220.0, 95.0, 120.0, 240.0, 2400, 240.0, false, false, 0, 650, 0);
        changed |= this.setNuclearTierDefaults(8, "Tsar Bomba", 50000.0, 320.0, 86.0, 58, 30, 38, 280.0, 125.0, 150.0, 300.0, 3000, 320.0, false, false, 0, 650, 0);
        changed |= this.setNuclearTierDefaults(9, "Tsar Bomba MAX", 100000.0, 410.0, 104.0, 68, 36, 46, 340.0, 155.0, 180.0, 360.0, 3200, 410.0, false, false, 0, 650, 0);
        changed |= this.setNuclearTierDefaults(10, "World Eater", 1000000.0, 520.0, 128.0, 86, 44, 58, 430.0, 210.0, 220.0, 520.0, 3600, 700.0, true, true, 1000, 1200, 600000);
        changed |= this.setIfMissing("messages.nuclear-arming", "&4⚠ Ядерная бомба: детонация через &f{seconds}&4 сек.");
        changed |= this.setIfMissing("messages.nuclear-detonated", "&4⚠ Ядерный взрыв. В кратере радиация!");
        changed |= this.setIfMissing("messages.radiation-warning", "&e⚠ Радиация: облучение &f{dose}/{threshold}");
        changed |= this.setIfMissing("messages.radiation-damage", "&c⚠ Критическое облучение: &f{dose}/{threshold}");
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

    private boolean setBombLevelDefaults(int level, String item, double power, int coreHits) {
        boolean changed = false;
        String prefix = "explosive.levels." + level;
        changed |= this.setIfMissing(prefix + ".item", item);
        changed |= this.setIfMissing(prefix + ".power", power);
        changed |= this.setIfMissing(prefix + ".core-hits", coreHits);
        return changed;
    }

    private boolean setNuclearTierDefaults(int level, String name, double yieldKilotons, double blastRadius, double explosionPower, int craterRadius, int craterDepth, int craterHeight, double mushroomHeight, double mushroomRadius, double radiationStartRadius, double radiationRadius, int radiationExpansionSeconds, double coreShockwaveRadius, boolean forbidden, boolean worldEater, int worldEaterRadius, int worldEaterBlocksPerTick, int worldEaterMaxBlocks) {
        boolean changed = false;
        String prefix = "explosive.nuclear-tiers." + level;
        changed |= this.setIfMissing(prefix + ".name", name);
        changed |= this.setIfMissing(prefix + ".yield-kilotons", yieldKilotons);
        changed |= this.setIfMissing(prefix + ".blast-radius", blastRadius);
        changed |= this.setIfMissing(prefix + ".explosion-power", explosionPower);
        changed |= this.setIfMissing(prefix + ".crater-radius", craterRadius);
        changed |= this.setIfMissing(prefix + ".crater-depth", craterDepth);
        changed |= this.setIfMissing(prefix + ".crater-height", craterHeight);
        changed |= this.setIfMissing(prefix + ".mushroom-height", mushroomHeight);
        changed |= this.setIfMissing(prefix + ".mushroom-radius", mushroomRadius);
        changed |= this.setIfMissing(prefix + ".radiation-start-radius", radiationStartRadius);
        changed |= this.setIfMissing(prefix + ".radiation-radius", radiationRadius);
        changed |= this.setIfMissing(prefix + ".radiation-expansion-seconds", radiationExpansionSeconds);
        changed |= this.setIfMissing(prefix + ".core-shockwave-radius", coreShockwaveRadius);
        changed |= this.setIfMissing(prefix + ".forbidden", forbidden);
        changed |= this.setIfMissing(prefix + ".world-eater", worldEater);
        if (worldEater) {
            changed |= this.setIfMissing(prefix + ".world-eater-radius", worldEaterRadius);
            changed |= this.setIfMissing(prefix + ".world-eater-blocks-per-tick", worldEaterBlocksPerTick);
            changed |= this.setIfMissing(prefix + ".world-eater-max-blocks", worldEaterMaxBlocks);
        }
        return changed;
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

    public EconomyService getEconomyService() {
        return this.economyService;
    }

    public UpgradeMenuListener getUpgradeMenuListener() {
        return this.upgradeMenuListener;
    }
}
