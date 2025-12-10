package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoostManager {

    private final RoyalEconomy plugin;
    private final double defaultMultiplier;
    private final Map<String, Double> permissionMultipliers = new HashMap<>();

    public BoostManager(RoyalEconomy plugin) {
        this.plugin = plugin;

        var cfg = plugin.getConfig();
        this.defaultMultiplier = cfg.getDouble("boosts.default-multiplier", 1.0);

        ConfigurationSection sec = cfg.getConfigurationSection("boosts.permission-multipliers");
        if (sec != null) {
            for (String perm : sec.getKeys(false)) {
                double value = sec.getDouble(perm, 1.0);
                permissionMultipliers.put(perm, value);
            }
        }
    }

    public double getMultiplier(UUID uuid) {
        if (!plugin.getConfig().getBoolean("boosts.enabled", false)) {
            return 1.0;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (!op.isOnline()) {
            return defaultMultiplier;
        }

        double best = defaultMultiplier;

        for (Map.Entry<String, Double> entry : permissionMultipliers.entrySet()) {
            String perm = entry.getKey();
            double mult = entry.getValue();
            if (op.getPlayer().hasPermission(perm)) {
                if (mult > best) best = mult;
            }
        }

        return best;
    }

    /**
     * Returns boosted amount; also logs if multiplier != 1.
     */
    public double applyBoost(UUID uuid, double baseAmount, String reason) {
        double mult = getMultiplier(uuid);
        if (mult <= 0) mult = 1.0; // safety
        double boosted = baseAmount * mult;

        if (mult != 1.0) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            plugin.getTransactionLogger().log(
                    "BOOST",
                    name,
                    boosted - baseAmount,
                    "BOOST_" + reason,
                    true
            );
        }

        return boosted;
    }
}
