package com.ArkOfNoah.RoyalEconomy.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.stream.Collectors;

public class OraxenHook {

    // Removed the static 'enabled' boolean.
    // We check Bukkit.getPluginManager() dynamically now.

    /**
     * Initializes the hook (Just for logging purposes now)
     */
    public static void init() {
        if (isOraxenReady()) {
            Bukkit.getLogger().info("[RoyalEconomy] Hooked into Oraxen successfully.");
        } else {
            Bukkit.getLogger().info("[RoyalEconomy] Oraxen not currently active. Hook will activate automatically when ready.");
        }
    }

    /**
     * Checks if Oraxen is enabled and ready to use.
     */
    private static boolean isOraxenReady() {
        return Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

    public static ItemStack getItem(String oraxenId, Material fallback) {
        if (isOraxenReady() && oraxenId != null && OraxenItems.exists(oraxenId)) {
            return OraxenItems.getItemById(oraxenId).build();
        }
        return new ItemStack(fallback);
    }

    public static String getGlyph(String glyphId) {
        // 1. Dynamic Check: Is Oraxen loaded NOW?
        if (!isOraxenReady()) {
            // Don't spam warnings, just return empty so the plugin doesn't crash
            return "";
        }

        if (glyphId == null) return "";

        try {
            // 2. Get Manager (Safe check for nulls)
            OraxenPlugin oraxen = OraxenPlugin.get();
            if (oraxen == null) return "";

            FontManager fontManager = oraxen.getFontManager();
            if (fontManager == null) return "";

            // 3. Look for the Glyph
            Collection<Glyph> glyphs = fontManager.getGlyphs();
            if (glyphs != null) {
                for (Glyph glyph : glyphs) {
                    if (glyph.getName().equals(glyphId)) {
                        return String.valueOf(glyph.getCharacter());
                    }
                }
            }

            // --- DEBUG SECTION (Only runs if glyph is missing) ---
            Bukkit.getLogger().warning("[RoyalEconomy] Glyph not found: " + glyphId);
            if (glyphs != null) {
                String suggestion = glyphs.stream()
                        .map(Glyph::getName)
                        .filter(name -> name.contains("bank") || name.contains("shift") || name.contains("neg"))
                        .limit(5)
                        .collect(Collectors.joining(", "));
                if (!suggestion.isEmpty()) {
                    Bukkit.getLogger().info("Did you mean? -> " + suggestion);
                }
            }

        } catch (Exception e) {
            // Suppress errors during reload
        }
        return "";
    }

    public static String getShift(int pixels) {
        if (!isOraxenReady()) return "";
        try {
            FontManager fontManager = OraxenPlugin.get().getFontManager();
            // This method automatically returns the special unicode string
            // that moves the cursor by 'pixels' amount.
            return fontManager.getShift(pixels);
        } catch (Exception e) {
            return "";
        }
    }
}