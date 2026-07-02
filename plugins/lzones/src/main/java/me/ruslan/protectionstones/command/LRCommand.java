/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package me.ruslan.protectionstones.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class LRCommand
implements CommandExecutor,
TabCompleter {
    private final ProtectionStonesPlugin plugin;

    public LRCommand(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        Player player = (Player)sender;
        if ("private".equalsIgnoreCase(command.getName())) {
            return this.handlePrivateCommand(player, args);
        }
        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }
        return switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> this.handleInfo(player);
            case "list" -> this.handleList(player);
            case "show" -> this.handleShow(player);
            case "remove" -> this.handleRemove(player);
            case "home" -> this.handleHome(player, args);
            case "add", "trust" -> this.handleAddMember(player, args);
            case "removeplayer", "untrust", "delmember" -> this.handleRemoveMember(player, args);
            case "members" -> this.handleMembers(player, args);
            case "particle", "visual" -> this.handleVisual(player, args);
            case "reload" -> this.handleReload(player);
            case "admin" -> this.handleAdmin(player);
            case "give" -> this.handleGive(player, args);
            default -> {
                this.sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handlePrivateCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(this.plugin.getMessageService().component("&7/private home [name]"));
            return true;
        }
        if ("home".equalsIgnoreCase(args[0])) {
            return this.handleHome(player, args);
        }
        player.sendMessage(this.plugin.getMessageService().component("&7/private home [name]"));
        return true;
    }

    private boolean handleInfo(Player player) {
        Region region = this.plugin.getRegionService().regionAt(player.getLocation()).orElse(null);
        if (region == null) {
            this.plugin.getMessageService().send(player, "no-region", Map.of());
            return true;
        }
        Settings.ProtectionTier shellTier = this.plugin.getSettings().tierById(region.tierId()).orElse(null);
        this.plugin.getMessageService().send(player, "region-info", Map.of("name", region.name(), "owner", region.ownerName(), "level", this.plugin.getItemService().roman(region.bookLevel()), "radius", Integer.toString(region.radius()), "damage", Integer.toString(region.tntDamage()), "max_damage", shellTier == null ? "?" : Integer.toString(shellTier.tntHitsToBreak())));
        return true;
    }

    private boolean handleList(Player player) {
        List<Region> regions = this.plugin.getRegionService().ownedBy(player.getUniqueId());
        if (regions.isEmpty()) {
            this.plugin.getMessageService().send(player, "region-list-empty", Map.of());
            return true;
        }
        for (Region region : regions) {
            Settings.ProtectionTier shellTier = this.plugin.getSettings().tierById(region.tierId()).orElse(null);
            player.sendMessage(this.plugin.getMessageService().component("&7- &b" + region.name() + "&7 | \u043a\u043d\u0438\u0433\u0430: &f" + this.plugin.getItemService().roman(region.bookLevel()) + "&7 | \u0440\u0430\u0434\u0438\u0443\u0441: &f" + region.radius() + "&7 | \u043e\u0431\u043e\u043b\u043e\u0447\u043a\u0430: &f" + (shellTier == null ? "?" : shellTier.blockMaterial().name()) + "&7 | \u043c\u0438\u0440: &f" + region.worldName() + "&7 | X: &f" + region.coreLocation().getBlockX() + "&7 Z: &f" + region.coreLocation().getBlockZ()));
        }
        return true;
    }

    private boolean handleShow(Player player) {
        Optional<Region> region = this.plugin.getRegionService().currentOrNearestOwned(player);
        if (region.isEmpty()) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        this.plugin.getOutlineService().showRegion(player, region.orElseThrow());
        this.plugin.getMessageService().send(player, "outline-shown", Map.of());
        return true;
    }

    private boolean handleRemove(Player player) {
        Optional<Region> regionOptional = this.plugin.getRegionService().currentOrNearestOwned(player);
        if (regionOptional.isEmpty()) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        Region region = regionOptional.orElseThrow();
        Settings.ProtectionTier shellTier = this.plugin.getSettings().tierById(region.tierId()).orElse(null);
        this.plugin.getRegionService().remove(region);
        this.plugin.saveRegionsQuietly();
        if (shellTier != null) {
            player.getWorld().dropItemNaturally(player.getLocation(), this.plugin.getItemService().createProtectionCore(shellTier, region.bookLevel()));
        }
        this.plugin.getMessageService().send(player, "region-removed", Map.of());
        return true;
    }

    private boolean handleHome(Player player, String[] args) {
        Optional<Region> region;
        Optional<Region> optional = region = args.length >= 2 ? this.plugin.getRegionService().ownedRegionByName(player, args[1]) : this.plugin.getRegionService().currentOrNearestOwned(player);
        if (region.isEmpty()) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        Location home = this.plugin.getRegionService().findHomeLocation(region.orElseThrow());
        if (home.getWorld() == null) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        player.teleport(home);
        player.playSound(home, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.1f);
        this.plugin.getMessageService().send(player, "home-teleport", Map.of("name", region.orElseThrow().name()));
        return true;
    }

    private boolean handleAddMember(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr add <player> [region]"));
            return true;
        }
        Optional<Region> regionOptional = this.resolveOwnedRegion(player, args.length >= 3 ? args[2] : null);
        if (regionOptional.isEmpty()) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        Region region = regionOptional.orElseThrow();
        OfflinePlayer target = this.resolveOfflinePlayer(args[1]);
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName() == null || target.getName().isBlank() ? args[1] : target.getName();
        if (region.isOwner(targetUuid)) {
            this.plugin.getMessageService().send(player, "member-owner", Map.of("player", targetName));
            return true;
        }
        if (region.isMember(targetUuid)) {
            this.plugin.getMessageService().send(player, "member-already-added", Map.of("player", targetName));
            return true;
        }
        int maxMembers = this.plugin.getSettings().maxMembersPerRegion();
        if (region.memberCount() >= maxMembers) {
            this.plugin.getMessageService().send(player, "member-limit-reached", Map.of("max", Integer.toString(maxMembers)));
            return true;
        }
        region.addMember(targetUuid, targetName, maxMembers);
        this.plugin.saveRegionsQuietly();
        this.plugin.getMessageService().send(player, "member-added", Map.of("player", targetName, "region", region.name(), "count", Integer.toString(region.memberCount()), "max", Integer.toString(maxMembers)));
        return true;
    }

    private boolean handleRemoveMember(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr removeplayer <player> [region]"));
            return true;
        }
        Optional<Region> regionOptional = this.resolveOwnedRegion(player, args.length >= 3 ? args[2] : null);
        if (regionOptional.isEmpty()) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        Region region = regionOptional.orElseThrow();
        Optional<UUID> storedMember = region.memberUuidByName(args[1]);
        UUID targetUuid = storedMember.orElseGet(() -> this.resolveOfflinePlayer(args[1]).getUniqueId());
        String targetName = region.members().getOrDefault(targetUuid, args[1]);
        if (!region.removeMember(targetUuid)) {
            this.plugin.getMessageService().send(player, "member-not-found", Map.of("player", args[1]));
            return true;
        }
        this.plugin.saveRegionsQuietly();
        this.plugin.getMessageService().send(player, "member-removed", Map.of("player", targetName, "region", region.name()));
        return true;
    }

    private boolean handleMembers(Player player, String[] args) {
        Optional<Region> regionOptional = this.resolveOwnedRegion(player, args.length >= 2 ? args[1] : null);
        if (regionOptional.isEmpty()) {
            this.plugin.getMessageService().send(player, "no-near-region", Map.of());
            return true;
        }
        Region region = regionOptional.orElseThrow();
        if (region.memberCount() == 0) {
            this.plugin.getMessageService().send(player, "member-list-empty", Map.of("region", region.name()));
            return true;
        }
        ArrayList<String> names = new ArrayList<String>(region.members().values());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        this.plugin.getMessageService().send(player, "member-list", Map.of("region", region.name(), "members", String.join(", ", names), "count", Integer.toString(region.memberCount()), "max", Integer.toString(this.plugin.getSettings().maxMembersPerRegion())));
        return true;
    }

    private Optional<Region> resolveOwnedRegion(Player player, String regionName) {
        if (regionName != null && !regionName.isBlank()) {
            return this.plugin.getRegionService().ownedRegionByName(player, regionName);
        }
        return this.plugin.getRegionService().currentOrNearestOwned(player);
    }

    private OfflinePlayer resolveOfflinePlayer(String name) {
        Player online = Bukkit.getPlayerExact((String)name);
        return online != null ? online : Bukkit.getOfflinePlayer((String)name);
    }

    private boolean handleVisual(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr particle <on|off>"));
            return true;
        }
        if ("on".equalsIgnoreCase(args[1])) {
            this.plugin.setVisualizationEnabled(player, true);
            this.plugin.getMessageService().send(player, "particles-enabled", Map.of());
            return true;
        }
        if ("off".equalsIgnoreCase(args[1])) {
            this.plugin.setVisualizationEnabled(player, false);
            this.plugin.getMessageService().send(player, "particles-disabled", Map.of());
            return true;
        }
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("protectionstones.reload")) {
            return true;
        }
        this.plugin.reloadLocalConfig();
        this.plugin.getMessageService().send(player, "reload-done", Map.of());
        this.plugin.getMessageService().send(player, "recipe-note", Map.of());
        return true;
    }

    private boolean handleAdmin(Player player) {
        if (!player.hasPermission("protectionstones.admin")) {
            return true;
        }
        boolean enabled = this.plugin.toggleAdminMode(player);
        this.plugin.getMessageService().send(player, enabled ? "admin-enabled" : "admin-disabled", Map.of());
        return true;
    }

    private boolean handleGive(Player player, String[] args) {
        if (!player.hasPermission("protectionstones.give")) {
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr give <core|book|bomb> <value> [book-level/player] [player]"));
            return true;
        }
        ItemStack stack = null;
        Player target = player;
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "core": {
                Settings.ProtectionTier shellTier = this.plugin.getSettings().tierById(args[2]).orElse(null);
                if (shellTier == null) break;
                int bookLevel = shellTier.level();
                if (args.length >= 4 && this.isInt(args[3])) {
                    bookLevel = this.parseInt(args[3], shellTier.level());
                    if (args.length >= 5) {
                        target = Bukkit.getPlayerExact((String)args[4]);
                    }
                } else if (args.length >= 4) {
                    target = Bukkit.getPlayerExact((String)args[3]);
                }
                stack = this.plugin.getItemService().createProtectionCore(shellTier, bookLevel);
                break;
            }
            case "book": {
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact((String)args[3]);
                }
                stack = this.plugin.getItemService().createZoneBook(this.parseInt(args[2], 1));
                break;
            }
            case "bomb": {
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact((String)args[3]);
                }
                stack = this.plugin.getSettings().bombTier(this.parseInt(args[2], 1)).map(this.plugin.getItemService()::createBomb).orElse(null);
                break;
            }
        }
        if (target == null) {
            player.sendMessage(this.plugin.getMessageService().component("&c\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d"));
            return true;
        }
        if (stack == null) {
            player.sendMessage(this.plugin.getMessageService().component("&c\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043f\u0440\u0435\u0434\u043c\u0435\u0442"));
            return true;
        }
        target.getInventory().addItem(new ItemStack[]{stack});
        this.plugin.getMessageService().send(player, "given-item", Map.of("item", this.plugin.getItemService().describe(stack)));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(this.plugin.getMessageService().component("&6=== LZones \u043a\u043e\u043c\u0430\u043d\u0434\u044b ==="));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr info &e- \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f \u043e \u0442\u0435\u043a\u0443\u0449\u0435\u043c \u0440\u0435\u0433\u0438\u043e\u043d\u0435"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr list &e- \u0441\u043f\u0438\u0441\u043e\u043a \u0432\u0430\u0448\u0438\u0445 \u0440\u0435\u0433\u0438\u043e\u043d\u043e\u0432"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr show &e- \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u0433\u0440\u0430\u043d\u0438\u0446\u044b \u043b\u0438\u043d\u0438\u044f\u043c\u0438"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr remove &e- \u0443\u0431\u0440\u0430\u0442\u044c \u0442\u0435\u043a\u0443\u0449\u0438\u0439 \u0438\u043b\u0438 \u0431\u043b\u0438\u0436\u0430\u0439\u0448\u0438\u0439 \u0440\u0435\u0433\u0438\u043e\u043d"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr home &e- \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442 \u043d\u0430 home \u0440\u0435\u0433\u0438\u043e\u043d\u0430"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr add <player> [region] &e- \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0430"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr removeplayer <player> [region] &e- \u0443\u0431\u0440\u0430\u0442\u044c \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0430"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr members [region] &e- \u0441\u043f\u0438\u0441\u043e\u043a \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u043e\u0432"));
        player.sendMessage(this.plugin.getMessageService().component("&7/private home [name] &e- \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u044b\u0439 \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442 \u043d\u0430\u0434 \u044f\u0434\u0440\u043e\u043c"));
        player.sendMessage(this.plugin.getMessageService().component("&7/lr particle <on|off> &e- \u0432\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0438\u043b\u0438 \u0432\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0432\u0438\u0437\u0443\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u044e"));
        if (player.hasPermission("protectionstones.admin")) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr admin &e- \u0430\u0434\u043c\u0438\u043d-\u0440\u0435\u0436\u0438\u043c"));
        }
        if (player.hasPermission("protectionstones.reload")) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr reload &e- \u043f\u0435\u0440\u0435\u0447\u0438\u0442\u0430\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433"));
        }
        if (player.hasPermission("protectionstones.give")) {
            player.sendMessage(this.plugin.getMessageService().component("&7/lr give core <tier> [book-level] [player]"));
            player.sendMessage(this.plugin.getMessageService().component("&7/lr give book <1-5> [player]"));
            player.sendMessage(this.plugin.getMessageService().component("&7/lr give bomb <1-5> [player]"));
        }
    }

    private boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        }
        catch (NumberFormatException ignored) {
            return false;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("private".equalsIgnoreCase(command.getName())) {
            return this.tabCompletePrivate(sender, args);
        }
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            completions.add("info");
            completions.add("list");
            completions.add("show");
            completions.add("remove");
            completions.add("home");
            completions.add("add");
            completions.add("removeplayer");
            completions.add("members");
            completions.add("particle");
            if (sender.hasPermission("protectionstones.admin")) {
                completions.add("admin");
            }
            if (sender.hasPermission("protectionstones.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("protectionstones.give")) {
                completions.add("give");
            }
        } else if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            completions.add("core");
            completions.add("book");
            completions.add("bomb");
        } else if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            if ("core".equalsIgnoreCase(args[1])) {
                completions.addAll(this.plugin.getSettings().protectionTiers().keySet());
            } else if ("book".equalsIgnoreCase(args[1])) {
                completions.addAll(List.of("1", "2", "3", "4", "5"));
            } else if ("bomb".equalsIgnoreCase(args[1])) {
                completions.addAll(this.plugin.getSettings().bombTiers().keySet().stream().map(String::valueOf).toList());
            }
        } else if (args.length == 4 && "give".equalsIgnoreCase(args[0]) && "core".equalsIgnoreCase(args[1])) {
            completions.addAll(List.of("1", "2", "3", "4", "5"));
        } else if (args.length == 2 && ("particle".equalsIgnoreCase(args[0]) || "visual".equalsIgnoreCase(args[0]))) {
            completions.add("on");
            completions.add("off");
        } else if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "trust".equalsIgnoreCase(args[0]))) {
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        } else if (args.length == 2 && ("removeplayer".equalsIgnoreCase(args[0]) || "untrust".equalsIgnoreCase(args[0]) || "delmember".equalsIgnoreCase(args[0])) && sender instanceof Player) {
            Player player = (Player)sender;
            this.plugin.getRegionService().currentOrNearestOwned(player).ifPresent(region -> completions.addAll(region.members().values()));
        } else if (args.length == 3 && ("add".equalsIgnoreCase(args[0]) || "trust".equalsIgnoreCase(args[0]) || "removeplayer".equalsIgnoreCase(args[0]) || "untrust".equalsIgnoreCase(args[0]) || "delmember".equalsIgnoreCase(args[0])) && sender instanceof Player) {
            Player player = (Player)sender;
            completions.addAll(this.plugin.getRegionService().ownedBy(player.getUniqueId()).stream().map(Region::name).toList());
        } else if (args.length == 2 && "members".equalsIgnoreCase(args[0]) && sender instanceof Player) {
            Player player = (Player)sender;
            completions.addAll(this.plugin.getRegionService().ownedBy(player.getUniqueId()).stream().map(Region::name).toList());
        } else if (args.length == 2 && "home".equalsIgnoreCase(args[0]) && sender instanceof Player) {
            Player player = (Player)sender;
            completions.addAll(this.plugin.getRegionService().ownedBy(player.getUniqueId()).stream().map(Region::name).toList());
        }
        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        completions.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(current));
        return completions;
    }

    private List<String> tabCompletePrivate(CommandSender sender, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            completions.add("home");
        } else if (args.length == 2 && "home".equalsIgnoreCase(args[0]) && sender instanceof Player) {
            Player player = (Player)sender;
            completions.addAll(this.plugin.getRegionService().ownedBy(player.getUniqueId()).stream().map(Region::name).toList());
        }
        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        completions.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(current));
        return completions;
    }
}
