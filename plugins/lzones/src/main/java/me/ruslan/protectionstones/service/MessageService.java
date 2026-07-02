/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.entity.Player
 */
package me.ruslan.protectionstones.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class MessageService {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private final ProtectionStonesPlugin plugin;
    private final Map<UUID, Map<String, Long>> actionBarCooldowns = new ConcurrentHashMap<UUID, Map<String, Long>>();

    public MessageService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player player, String key) {
        this.send(player, key, Map.of());
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        if (player == null) {
            return;
        }
        player.sendMessage(this.component(this.message(key, placeholders)));
    }

    public void actionBar(Player player, String key, Map<String, String> placeholders) {
        this.actionBar(player, key, key, placeholders);
    }

    public void actionBar(Player player, String key, String messageKey, Map<String, String> placeholders) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMillis = Math.max(50L, (long)this.plugin.getSettings().actionBarCooldownTicks() * 50L);
        Map<String, Long> playerMap = this.actionBarCooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        Long last = playerMap.get(key);
        if (last != null && now - last < cooldownMillis) {
            return;
        }
        playerMap.put(key, now);
        player.sendActionBar(this.component(this.stripPrefix(this.message(messageKey, placeholders))));
    }

    public String message(String key, Map<String, String> placeholders) {
        Settings settings = this.plugin.getSettings();
        String resolved = settings.message(key);
        String prefix = settings.message("prefix");
        String text = resolved;
        if (!"prefix".equals(key) && !resolved.startsWith(prefix)) {
            text = prefix + resolved;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    public Component component(String text) {
        return SERIALIZER.deserialize(text);
    }

    public void clearPlayer(Player player) {
        if (player != null) {
            this.actionBarCooldowns.remove(player.getUniqueId());
        }
    }

    private String stripPrefix(String value) {
        String prefix = this.plugin.getSettings().message("prefix");
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
