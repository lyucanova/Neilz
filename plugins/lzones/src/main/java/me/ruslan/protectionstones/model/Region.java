/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 */
package me.ruslan.protectionstones.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Region {
    private final String name;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String worldName;
    private final int coreX;
    private final int coreY;
    private final int coreZ;
    private int radius;
    private int minY;
    private int maxY;
    private String tierId;
    private int bookLevel;
    private int tntDamage;
    private final LinkedHashMap<UUID, String> members;

    public Region(String name, UUID ownerUuid, String ownerName, String worldName, int coreX, int coreY, int coreZ, int radius, int minY, int maxY, String tierId, int bookLevel, int tntDamage) {
        this(name, ownerUuid, ownerName, worldName, coreX, coreY, coreZ, radius, minY, maxY, tierId, bookLevel, tntDamage, Map.of());
    }

    public Region(String name, UUID ownerUuid, String ownerName, String worldName, int coreX, int coreY, int coreZ, int radius, int minY, int maxY, String tierId, int bookLevel, int tntDamage, Map<UUID, String> members) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.coreX = coreX;
        this.coreY = coreY;
        this.coreZ = coreZ;
        this.radius = radius;
        this.minY = minY;
        this.maxY = maxY;
        this.tierId = tierId;
        this.bookLevel = Math.max(1, bookLevel);
        this.tntDamage = tntDamage;
        this.members = new LinkedHashMap<UUID, String>();
        if (members != null) {
            for (Map.Entry<UUID, String> entry : members.entrySet()) {
                UUID uuid = entry.getKey();
                if (uuid == null || uuid.equals(ownerUuid)) {
                    continue;
                }
                this.members.put(uuid, entry.getValue() == null || entry.getValue().isBlank() ? uuid.toString() : entry.getValue());
            }
        }
    }

    public String name() {
        return this.name;
    }

    public UUID ownerUuid() {
        return this.ownerUuid;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public String worldName() {
        return this.worldName;
    }

    public int radius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int minY() {
        return this.minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int maxY() {
        return this.maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public String tierId() {
        return this.tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public int bookLevel() {
        return this.bookLevel;
    }

    public void setBookLevel(int bookLevel) {
        this.bookLevel = Math.max(1, bookLevel);
    }

    public int tntDamage() {
        return this.tntDamage;
    }

    public void setTntDamage(int tntDamage) {
        this.tntDamage = Math.max(0, tntDamage);
    }

    public boolean isOwner(UUID uuid) {
        return uuid != null && this.ownerUuid.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return uuid != null && this.members.containsKey(uuid);
    }

    public boolean canBuild(UUID uuid) {
        return this.isOwner(uuid) || this.isMember(uuid);
    }

    public int memberCount() {
        return this.members.size();
    }

    public Map<UUID, String> members() {
        return Map.copyOf(this.members);
    }

    public Optional<UUID> memberUuidByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return this.members.entrySet().stream().filter(entry -> entry.getValue().equalsIgnoreCase(name)).map(Map.Entry::getKey).findFirst();
    }

    public boolean addMember(UUID uuid, String name, int maxMembers) {
        if (uuid == null || this.isOwner(uuid)) {
            return false;
        }
        if (this.members.containsKey(uuid)) {
            this.members.put(uuid, name == null || name.isBlank() ? this.members.get(uuid) : name);
            return false;
        }
        if (this.members.size() >= maxMembers) {
            return false;
        }
        this.members.put(uuid, name == null || name.isBlank() ? uuid.toString() : name);
        return true;
    }

    public boolean removeMember(UUID uuid) {
        return uuid != null && this.members.remove(uuid) != null;
    }

    public void incrementTntDamage() {
        ++this.tntDamage;
    }

    public int minX() {
        return this.coreX - this.radius;
    }

    public int maxX() {
        return this.coreX + this.radius;
    }

    public int minZ() {
        return this.coreZ - this.radius;
    }

    public int maxZ() {
        return this.coreZ + this.radius;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!Objects.equals(location.getWorld().getName(), this.worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= this.minX() && x <= this.maxX() && z >= this.minZ() && z <= this.maxZ() && y >= this.minY && y <= this.maxY;
    }

    public boolean isCore(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return Objects.equals(location.getWorld().getName(), this.worldName) && location.getBlockX() == this.coreX && location.getBlockY() == this.coreY && location.getBlockZ() == this.coreZ;
    }

    public Location coreLocation() {
        World world = Bukkit.getWorld((String)this.worldName);
        return new Location(world, (double)this.coreX, (double)this.coreY, (double)this.coreZ);
    }

    public String coreKey() {
        return this.worldName + ":" + this.coreX + ":" + this.coreY + ":" + this.coreZ;
    }

    public Map<String, Object> serialize() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", this.name);
        map.put("ownerUuid", this.ownerUuid.toString());
        map.put("ownerName", this.ownerName);
        map.put("world", this.worldName);
        map.put("coreX", this.coreX);
        map.put("coreY", this.coreY);
        map.put("coreZ", this.coreZ);
        map.put("radius", this.radius);
        map.put("minY", this.minY);
        map.put("maxY", this.maxY);
        map.put("tierId", this.tierId);
        map.put("bookLevel", this.bookLevel);
        map.put("tntDamage", this.tntDamage);
        List<Map<String, Object>> serializedMembers = new java.util.ArrayList<Map<String, Object>>();
        for (Map.Entry<UUID, String> entry : this.members.entrySet()) {
            LinkedHashMap<String, Object> member = new LinkedHashMap<String, Object>();
            member.put("uuid", entry.getKey().toString());
            member.put("name", entry.getValue());
            serializedMembers.add(member);
        }
        map.put("members", serializedMembers);
        return map;
    }

    public static Region deserialize(Map<?, ?> map) {
        String tierId = Region.stringValue(map, "tierId", "iron");
        UUID ownerUuid = UUID.fromString(Region.stringValue(map, "ownerUuid", new UUID(0L, 0L).toString()));
        return new Region(Region.stringValue(map, "name", "zone"), ownerUuid, Region.stringValue(map, "ownerName", "Unknown"), Region.stringValue(map, "world", "world"), Region.number(map.get("coreX")), Region.number(map.get("coreY")), Region.number(map.get("coreZ")), Region.number(map.get("radius")), Region.number(map.get("minY")), Region.number(map.get("maxY")), tierId, Region.number(map.containsKey("bookLevel") ? map.get("bookLevel") : Integer.valueOf(Region.inferBookLevel(tierId))), Region.number(map.containsKey("tntDamage") ? map.get("tntDamage") : Integer.valueOf(0)), Region.deserializeMembers(map.get("members"), ownerUuid));
    }

    private static LinkedHashMap<UUID, String> deserializeMembers(Object rawMembers, UUID ownerUuid) {
        LinkedHashMap<UUID, String> members = new LinkedHashMap<UUID, String>();
        if (!(rawMembers instanceof Iterable<?> iterable)) {
            return members;
        }
        for (Object rawMember : iterable) {
            UUID uuid = null;
            String name = null;
            if (rawMember instanceof Map<?, ?> memberMap) {
                uuid = Region.parseUuid(memberMap.get("uuid"));
                name = memberMap.containsKey("name") ? String.valueOf(memberMap.get("name")) : null;
            } else {
                uuid = Region.parseUuid(rawMember);
            }
            if (uuid == null || uuid.equals(ownerUuid)) {
                continue;
            }
            members.put(uuid, name == null || name.isBlank() ? uuid.toString() : name);
        }
        return members;
    }

    private static int inferBookLevel(String tierId) {
        return switch (tierId == null ? "" : tierId.toLowerCase()) {
            case "gold" -> 2;
            case "emerald" -> 3;
            case "diamond" -> 4;
            case "netherite" -> 5;
            default -> 1;
        };
    }

    private static String stringValue(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int number(Object value) {
        if (value instanceof Number) {
            Number number = (Number)value;
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
