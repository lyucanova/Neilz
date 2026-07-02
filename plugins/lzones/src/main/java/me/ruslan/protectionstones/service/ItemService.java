/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package me.ruslan.protectionstones.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class ItemService {
    private final ProtectionStonesPlugin plugin;
    private final NamespacedKey kindKey;
    private final NamespacedKey valueKey;
    private final NamespacedKey levelKey;
    private final Set<String> warnedEnchantKeys;

    public ItemService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
        this.kindKey = new NamespacedKey((Plugin)plugin, "kind");
        this.valueKey = new NamespacedKey((Plugin)plugin, "value");
        this.levelKey = new NamespacedKey((Plugin)plugin, "level");
        this.warnedEnchantKeys = new HashSet<String>();
    }

    public ItemStack createProtectionCore(Settings.ProtectionTier shellTier) {
        return this.createProtectionCore(shellTier, shellTier.level());
    }

    public ItemStack createProtectionCore(Settings.ProtectionTier shellTier, int bookLevel) {
        int normalizedBookLevel = this.normalizeBookLevel(bookLevel);
        int radius = this.plugin.getSettings().radiusForBookLevel(normalizedBookLevel);
        ItemStack item = new ItemStack(shellTier.blockMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u042f\u0434\u0440\u043e LZones \u00a7f" + this.roman(normalizedBookLevel));
        meta.setLore(List.of("\u00a77\u041c\u0430\u0442\u0435\u0440\u0438\u0430\u043b \u043e\u0431\u043e\u043b\u043e\u0447\u043a\u0438: \u00a7f" + this.prettify(shellTier.blockMaterial()), "\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c \u043a\u043d\u0438\u0433\u0438: \u00a7f" + this.roman(normalizedBookLevel), "\u00a77\u0420\u0430\u0434\u0438\u0443\u0441 \u043f\u0440\u0438\u0432\u0430\u0442\u0430: \u00a7f" + radius, "\u00a77\u0417\u0430\u043f\u0430\u0441 TNT: \u00a7f" + shellTier.tntHitsToBreak(), "\u00a77\u041e\u0431\u044b\u0447\u043d\u044b\u0439 \u0431\u043b\u043e\u043a \u043d\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442 \u0431\u0435\u0437 \u043a\u043d\u0438\u0433\u0438 \u0437\u043e\u043d\u044b", "\u00a77Shift + \u041f\u041a\u041c \u043f\u043e \u044f\u0434\u0440\u0443 \u043e\u0442\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u0435"));
        this.applyEnchant(meta, "unbreaking", Math.max(shellTier.level(), normalizedBookLevel), "protection core");
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.kindKey, PersistentDataType.STRING, "protection_core");
        pdc.set(this.valueKey, PersistentDataType.STRING, shellTier.id());
        pdc.set(this.levelKey, PersistentDataType.INTEGER, normalizedBookLevel);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createZoneBook(int level) {
        int normalizedLevel = this.normalizeBookLevel(level);
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7d\u041a\u043d\u0438\u0433\u0430 \u0437\u043e\u043d\u044b \u00a7f" + this.roman(normalizedLevel));
        meta.setLore(List.of("\u00a77\u0423\u0440\u043e\u0432\u0435\u043d\u044c \u0440\u0430\u0434\u0438\u0443\u0441\u0430: \u00a7f" + this.roman(normalizedLevel), "\u00a77\u041d\u0443\u0436\u043d\u0430 \u0434\u043b\u044f \u043a\u0440\u0430\u0444\u0442\u0430 \u0438 \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u044f \u044f\u0434\u0435\u0440", "\u00a77\u041e\u043f\u0440\u0435\u0434\u0435\u043b\u044f\u0435\u0442 \u0441\u0438\u043b\u0443 \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043d\u0438\u044f \u043f\u0440\u0438\u0432\u0430\u0442\u0430"));
        this.applyEnchant(meta, "protection", normalizedLevel, "zone book");
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.kindKey, PersistentDataType.STRING, "zone_book");
        pdc.set(this.levelKey, PersistentDataType.INTEGER, normalizedLevel);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBomb(Settings.BombTier tier) {
        ItemStack item = new ItemStack(tier.itemMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7c\u041e\u0441\u0430\u0434\u043d\u0430\u044f \u0441\u0444\u0435\u0440\u0430 \u00a7f" + this.roman(tier.level()));
        meta.setLore(List.of("\u00a77\u041c\u043e\u0449\u043d\u043e\u0441\u0442\u044c: \u00a7f" + tier.power(), "\u00a77\u0423\u0434\u0430\u0440\u043e\u0432 \u043f\u043e \u044f\u0434\u0440\u0443: \u00a7f" + tier.coreHits(), "\u00a77\u0411\u0435\u0437 \u043e\u043f\u043e\u0440\u044b \u043f\u0430\u0434\u0430\u0435\u0442 \u0432\u043d\u0438\u0437 \u043a\u0430\u043a \u0441\u043d\u0430\u0440\u044f\u0434", "\u00a77\u0412 \u042d\u043d\u0434\u0435 \u0432\u0437\u043b\u0435\u0442\u0430\u0435\u0442 \u0438 \u0431\u044c\u0451\u0442 \u0432 \u0434\u0432\u0430 \u0440\u0430\u0437\u0430 \u0441\u0438\u043b\u044c\u043d\u0435\u0435"));
        this.applyEnchant(meta, "fire_aspect", tier.level(), "explosive core");
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.kindKey, PersistentDataType.STRING, "explosive_core");
        pdc.set(this.levelKey, PersistentDataType.INTEGER, tier.level());
        item.setItemMeta(meta);
        return item;
    }

    public Optional<ProtectionCoreData> protectionCore(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!"protection_core".equals(pdc.get(this.kindKey, PersistentDataType.STRING))) {
            return Optional.empty();
        }
        String tierId = (String)pdc.get(this.valueKey, PersistentDataType.STRING);
        Integer bookLevel = (Integer)pdc.get(this.levelKey, PersistentDataType.INTEGER);
        Optional<Settings.ProtectionTier> shellTier = this.plugin.getSettings().tierById(tierId);
        if (shellTier.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ProtectionCoreData(shellTier.orElseThrow(), this.normalizeBookLevel(bookLevel == null ? 1 : bookLevel)));
    }

    public Optional<Settings.ProtectionTier> protectionTier(ItemStack item) {
        return this.protectionCore(item).map(ProtectionCoreData::shellTier);
    }

    public OptionalInt protectionBookLevel(ItemStack item) {
        return this.protectionCore(item).map(core -> OptionalInt.of(core.bookLevel())).orElseGet(OptionalInt::empty);
    }

    public OptionalInt zoneBookLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return OptionalInt.empty();
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!"zone_book".equals(pdc.get(this.kindKey, PersistentDataType.STRING))) {
            return OptionalInt.empty();
        }
        Integer level = (Integer)pdc.get(this.levelKey, PersistentDataType.INTEGER);
        return level == null ? OptionalInt.empty() : OptionalInt.of(this.normalizeBookLevel(level));
    }

    public Optional<Settings.BombTier> bombTier(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!"explosive_core".equals(pdc.get(this.kindKey, PersistentDataType.STRING))) {
            return Optional.empty();
        }
        Integer level = (Integer)pdc.get(this.levelKey, PersistentDataType.INTEGER);
        return level == null ? Optional.empty() : this.plugin.getSettings().bombTier(level);
    }

    public boolean looksLikeCoreMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return this.plugin.getSettings().tierByBlock(item.getType()).isPresent();
    }

    public String describe(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "AIR";
        }
        Optional<ProtectionCoreData> core = this.protectionCore(item);
        if (core.isPresent()) {
            ProtectionCoreData data = core.orElseThrow();
            return "\u042f\u0434\u0440\u043e " + this.prettify(data.shellTier().blockMaterial()) + " / \u043a\u043d\u0438\u0433\u0430 " + this.roman(data.bookLevel());
        }
        OptionalInt bookLevel = this.zoneBookLevel(item);
        if (bookLevel.isPresent()) {
            return "\u041a\u043d\u0438\u0433\u0430 \u0437\u043e\u043d\u044b " + this.roman(bookLevel.getAsInt());
        }
        Optional<Settings.BombTier> bombTier = this.bombTier(item);
        if (bombTier.isPresent()) {
            return "\u041e\u0441\u0430\u0434\u043d\u0430\u044f \u0441\u0444\u0435\u0440\u0430 " + this.roman(bombTier.orElseThrow().level());
        }
        return item.getType().name().toLowerCase(Locale.ROOT);
    }

    public int radiusForCore(ProtectionCoreData coreData) {
        return this.plugin.getSettings().radiusForBookLevel(coreData.bookLevel());
    }

    public String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(value);
        };
    }

    private int normalizeBookLevel(int level) {
        int highest = this.plugin.getSettings().highestProtectionLevel().orElse(1);
        return Math.max(1, Math.min(highest, level));
    }

    private String prettify(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private void applyEnchant(ItemMeta meta, String enchantKey, int level, String purpose) {
        Enchantment enchantment = this.requireEnchant(enchantKey, purpose);
        if (enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
    }

    private Enchantment requireEnchant(String enchantKey, String purpose) {
        Enchantment enchantment = Enchantment.getByKey((NamespacedKey)NamespacedKey.minecraft((String)enchantKey));
        if (enchantment == null && this.warnedEnchantKeys.add(enchantKey)) {
            this.plugin.getLogger().warning("Missing enchantment '" + enchantKey + "' for " + purpose + ". Item will be created without enchant glow.");
        }
        return enchantment;
    }

    public record ProtectionCoreData(Settings.ProtectionTier shellTier, int bookLevel) {
    }
}
