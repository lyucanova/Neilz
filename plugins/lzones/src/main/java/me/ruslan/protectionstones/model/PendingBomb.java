/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package me.ruslan.protectionstones.model;

import java.util.UUID;
import org.bukkit.Location;

public record PendingBomb(String key, UUID ownerUuid, Location location, int level, float power) {
}

