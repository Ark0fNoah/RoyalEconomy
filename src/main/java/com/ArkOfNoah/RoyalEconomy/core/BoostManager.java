package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BoostManager {

    private final RoyalEconomy plugin;

    public BoostManager(RoyalEconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates bonus interest based on permissions.
     */
    public double applyBoost(UUID uuid, double baseAmount, String type) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return baseAmount; // Only works for online players usually

        double multiplier = 1.0;

        // Example: check for permission "royaleconomy.boost.vip"
        // In config: boosts.vip: 1.5
        if (plugin.getConfig().isConfigurationSection("boosts")) {
            for (String key : plugin.getConfig().getConfigurationSection("boosts").getKeys(false)) {
                if (player.hasPermission("royaleconomy.boost." + key)) {
                    double boost = plugin.getConfig().getDouble("boosts." + key, 1.0);
                    if (boost > multiplier) {
                        multiplier = boost; // Take the highest multiplier
                    }
                }
            }
        }

        return baseAmount * multiplier;
    }
}