/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.Recipe
 *  org.bukkit.inventory.RecipeChoice
 *  org.bukkit.inventory.RecipeChoice$ExactChoice
 *  org.bukkit.inventory.ShapedRecipe
 *  org.bukkit.plugin.Plugin
 */
package me.ruslan.protectionstones.service;

import me.ruslan.protectionstones.ProtectionStonesPlugin;
import me.ruslan.protectionstones.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public final class RecipeService {
    private final ProtectionStonesPlugin plugin;

    public RecipeService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        this.registerProtectionCores();
    }

    private void registerProtectionCores() {
        int highestBookLevel = this.plugin.getSettings().highestProtectionLevel().orElse(1);
        for (Settings.ProtectionTier shellTier : this.plugin.getSettings().protectionTiers().values()) {
            if (!shellTier.craftable()) continue;
            for (int bookLevel = 1; bookLevel <= highestBookLevel; ++bookLevel) {
                this.registerCoreRecipe(shellTier, bookLevel);
            }
        }
    }

    private void registerCoreRecipe(Settings.ProtectionTier shellTier, int bookLevel) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey((Plugin)this.plugin, "protection_core_" + shellTier.id() + "_" + bookLevel), this.plugin.getItemService().createProtectionCore(shellTier, bookLevel));
        recipe.shape(new String[]{"MMM", "MBM", "MMM"});
        recipe.setIngredient('M', shellTier.blockMaterial());
        recipe.setIngredient('B', (RecipeChoice)new RecipeChoice.ExactChoice(this.plugin.getItemService().createZoneBook(bookLevel)));
        Bukkit.addRecipe((Recipe)recipe);
    }
}

