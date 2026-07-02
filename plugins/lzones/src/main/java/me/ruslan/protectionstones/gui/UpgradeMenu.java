/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.ruslan.protectionstones.gui;

import java.util.ArrayList;
import java.util.List;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import me.ruslan.protectionstones.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class UpgradeMenu {
    public static final String TITLE = "\u00a78\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u0435 \u0437\u043e\u043d\u044b";

    private UpgradeMenu() {
    }

    public static Inventory create(ProtectionStonesPlugin plugin, Player player, Region region) {
        Inventory inventory = Bukkit.createInventory(null, (int)27, (String)TITLE);
        Settings.ProtectionTier shellTier = plugin.getSettings().tierById(region.tierId()).orElse(null);
        Settings.ProtectionTier currentRadiusTier = plugin.getSettings().tierByLevel(region.bookLevel()).orElse(shellTier);
        Settings.ProtectionTier nextRadiusTier = plugin.getSettings().tierByLevel(region.bookLevel() + 1).orElse(null);
        if (shellTier == null || currentRadiusTier == null) {
            inventory.setItem(13, UpgradeMenu.named(Material.BARRIER, "\u00a7c\u041e\u0448\u0438\u0431\u043a\u0430 \u044f\u0434\u0440\u0430", List.of("\u00a77\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0438\u0442\u044c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0437\u043e\u043d\u044b")));
            return inventory;
        }
        inventory.setItem(10, UpgradeMenu.decorate(plugin.getItemService().createProtectionCore(shellTier, region.bookLevel()), List.of("\u00a77\u0418\u043c\u044f: \u00a7f" + region.name(), "\u00a77\u041c\u0430\u0442\u0435\u0440\u0438\u0430\u043b: \u00a7f" + UpgradeMenu.prettify(shellTier.blockMaterial()), "\u00a77\u0420\u0430\u0434\u0438\u0443\u0441 \u0441\u0435\u0439\u0447\u0430\u0441: \u00a7f" + region.radius(), "\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c \u043a\u043d\u0438\u0433\u0438: \u00a7f" + plugin.getItemService().roman(region.bookLevel()), "\u00a77\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c \u043a TNT: \u00a7f" + shellTier.tntHitsToBreak())));
        if (nextRadiusTier == null) {
            inventory.setItem(13, UpgradeMenu.named(Material.BARRIER, "\u00a7c\u041c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0440\u0430\u0434\u0438\u0443\u0441", List.of("\u00a77\u042d\u0442\u043e \u044f\u0434\u0440\u043e \u0443\u0436\u0435 \u0438\u043c\u0435\u0435\u0442 \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439", "\u00a77\u0443\u0440\u043e\u0432\u0435\u043d\u044c \u043a\u043d\u0438\u0433\u0438 \u0437\u043e\u043d\u044b.")));
            return inventory;
        }
        inventory.setItem(16, UpgradeMenu.decorate(plugin.getItemService().createProtectionCore(shellTier, nextRadiusTier.level()), List.of("\u00a77\u041c\u0430\u0442\u0435\u0440\u0438\u0430\u043b \u043e\u0441\u0442\u0430\u043d\u0435\u0442\u0441\u044f \u0442\u0435\u043c \u0436\u0435", "\u00a77\u041d\u043e\u0432\u044b\u0439 \u0440\u0430\u0434\u0438\u0443\u0441: \u00a7f" + nextRadiusTier.radius(), "\u00a77\u041d\u0443\u0436\u043d\u0430 \u043a\u043d\u0438\u0433\u0430: \u00a7f" + plugin.getItemService().roman(nextRadiusTier.level()))));
        inventory.setItem(13, UpgradeMenu.named(Material.ENCHANTED_BOOK, "\u00a7a\u041f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u043d\u0438\u0433\u0443 " + plugin.getItemService().roman(nextRadiusTier.level()), List.of("\u00a77\u0414\u043b\u044f \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u044f \u043d\u0443\u0436\u043d\u0430 \u0442\u043e\u043b\u044c\u043a\u043e", "\u00a77\u043a\u043d\u0438\u0433\u0430 \u0437\u043e\u043d\u044b \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0435\u0433\u043e \u0443\u0440\u043e\u0432\u043d\u044f.", "\u00a77\u041c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u044b \u043e\u0431\u043e\u043b\u043e\u0447\u043a\u0438 \u043d\u0435 \u0442\u0440\u0430\u0442\u044f\u0442\u0441\u044f.")));
        inventory.setItem(22, UpgradeMenu.named(Material.LIGHTNING_ROD, "\u00a7e\u041f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0430", List.of("\u00a77\u041c\u0430\u0442\u0435\u0440\u0438\u0430\u043b \u0431\u043b\u043e\u043a\u0430 \u043e\u0442\u0432\u0435\u0447\u0430\u0435\u0442 \u0437\u0430 \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c", "\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c \u043a\u043d\u0438\u0433\u0438 \u043e\u0442\u0432\u0435\u0447\u0430\u0435\u0442 \u0437\u0430 \u0440\u0430\u0434\u0438\u0443\u0441", "\u00a77Shift + \u041f\u041a\u041c \u043f\u043e \u044f\u0434\u0440\u0443 \u043e\u0442\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u044d\u0442\u043e \u043c\u0435\u043d\u044e")));
        return inventory;
    }

    private static ItemStack decorate(ItemStack item, List<String> extraLore) {
        ItemMeta meta = item.getItemMeta();
        ArrayList<String> lines = new ArrayList<String>();
        if (meta.hasLore()) {
            lines.addAll(meta.getLore());
        }
        lines.add("");
        lines.addAll(extraLore);
        meta.setLore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String prettify(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1)).append(part.substring(1).toLowerCase());
        }
        return builder.toString();
    }
}

