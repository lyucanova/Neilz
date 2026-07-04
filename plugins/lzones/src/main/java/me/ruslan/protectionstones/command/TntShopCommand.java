package me.ruslan.protectionstones.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class TntShopCommand implements CommandExecutor, TabCompleter {
    private static final Map<Integer, Double> DEFAULT_PRICES = new LinkedHashMap<Integer, Double>();
    private static final double DEFAULT_RADIATION_KIT_PRICE = 500000.0;

    static {
        DEFAULT_PRICES.put(1, 20000.0);
        DEFAULT_PRICES.put(2, 250000.0);
        DEFAULT_PRICES.put(3, 650000.0);
        DEFAULT_PRICES.put(4, 1000000.0);
        DEFAULT_PRICES.put(5, 15000000.0);
    }

    private final ProtectionStonesPlugin plugin;

    public TntShopCommand(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        if (!player.hasPermission("protectionstones.tntshop")) {
            return true;
        }
        if (args.length == 0) {
            this.sendShop(player);
            return true;
        }
        String levelArg = args.length >= 2 && this.isBuyAlias(args[0]) ? args[1] : args[0];
        if (this.isRadiationKitAlias(levelArg)) {
            this.buyRadiationKit(player);
            return true;
        }
        if (!this.isInt(levelArg)) {
            this.sendShop(player);
            return true;
        }
        int level = this.parseInt(levelArg, 0);
        if (!DEFAULT_PRICES.containsKey(level)) {
            player.sendMessage(this.plugin.getMessageService().component("&cВ магазине есть только TNT 1-5 уровня."));
            return true;
        }
        this.buy(player, level);
        return true;
    }

    private void sendShop(Player player) {
        player.sendMessage(this.plugin.getMessageService().component("&6=== TNT Shop ==="));
        for (int level : DEFAULT_PRICES.keySet()) {
            double price = this.price(level);
            player.sendMessage(this.plugin.getMessageService().component("&7/tntshop " + level + " &e- TNT " + this.plugin.getItemService().roman(level) + " &7| &a" + this.plugin.getEconomyService().format(price)));
        }
        player.sendMessage(this.plugin.getMessageService().component("&7/tntshop radkit &e- Радиационный комплект &7| &a" + this.plugin.getEconomyService().format(this.radiationKitPrice())));
    }

    private void buy(Player player, int level) {
        if (!this.plugin.getEconomyService().isAvailable()) {
            player.sendMessage(this.plugin.getMessageService().component("&cTNT Shop недоступен: нужен Vault и плагин экономики."));
            return;
        }
        Settings.BombTier tier = this.plugin.getSettings().bombTier(level).orElse(null);
        if (tier == null) {
            player.sendMessage(this.plugin.getMessageService().component("&cTNT " + level + " уровня не найден в конфиге."));
            return;
        }
        double price = this.price(level);
        if (!this.plugin.getEconomyService().has(player, price)) {
            player.sendMessage(this.plugin.getMessageService().component("&cНе хватает денег. Нужно: &f" + this.plugin.getEconomyService().format(price)));
            return;
        }
        if (!this.plugin.getEconomyService().withdraw(player, price)) {
            player.sendMessage(this.plugin.getMessageService().component("&cНе удалось списать деньги. Попробуй ещё раз."));
            return;
        }
        ItemStack bomb = this.plugin.getItemService().createBomb(tier);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack[]{bomb});
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.15f);
        player.sendMessage(this.plugin.getMessageService().component("&aКуплено: &fTNT " + this.plugin.getItemService().roman(level) + " &7за &a" + this.plugin.getEconomyService().format(price)));
    }

    private void buyRadiationKit(Player player) {
        if (!this.plugin.getEconomyService().isAvailable()) {
            player.sendMessage(this.plugin.getMessageService().component("&cTNT Shop недоступен: нужен Vault и плагин экономики."));
            return;
        }
        double price = this.radiationKitPrice();
        if (!this.plugin.getEconomyService().has(player, price)) {
            player.sendMessage(this.plugin.getMessageService().component("&cНе хватает денег. Нужно: &f" + this.plugin.getEconomyService().format(price)));
            return;
        }
        if (!this.plugin.getEconomyService().withdraw(player, price)) {
            player.sendMessage(this.plugin.getMessageService().component("&cНе удалось списать деньги. Попробуй ещё раз."));
            return;
        }
        for (ItemStack piece : this.plugin.getItemService().createRadiationSuit()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack[]{piece});
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.15f);
        player.sendMessage(this.plugin.getMessageService().component("&aКуплен радиационный комплект за &f" + this.plugin.getEconomyService().format(price)));
    }

    private double price(int level) {
        return this.plugin.getConfig().getDouble("shop.tnt-prices." + level, DEFAULT_PRICES.getOrDefault(level, 0.0));
    }

    private double radiationKitPrice() {
        return this.plugin.getConfig().getDouble("shop.radiation-kit-price", DEFAULT_RADIATION_KIT_PRICE);
    }

    private boolean isBuyAlias(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("buy") || normalized.equals("b") || normalized.equals("купить");
    }

    private boolean isRadiationKitAlias(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("radkit") || normalized.equals("radiation") || normalized.equals("radiationkit");
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
        ArrayList<String> completions = new ArrayList<String>();
        if (!sender.hasPermission("protectionstones.tntshop")) {
            return completions;
        }
        if (args.length == 1) {
            completions.add("1");
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
            completions.add("radkit");
            completions.add("buy");
        } else if (args.length == 2 && this.isBuyAlias(args[0])) {
            completions.add("1");
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
            completions.add("radkit");
        }
        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        completions.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(current));
        return completions;
    }
}
