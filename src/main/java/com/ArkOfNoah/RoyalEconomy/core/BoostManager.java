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

    private static class TempBoost {
        double multiplier;
        long expiresAt; // epoch millis
    }

    private final Map<UUID, TempBoost> tempBoosts = new HashMap<>();

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

    public synchronized double getMultiplier(UUID uuid) {
        double best = defaultMultiplier;

        if (plugin.getConfig().getBoolean("boosts.enabled", false)) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.isOnline() && op.getPlayer() != null) {
                for (Map.Entry<String, Double> entry : permissionMultipliers.entrySet()) {
                    String perm = entry.getKey();
                    double mult = entry.getValue();
                    if (op.getPlayer().hasPermission(perm)) {
                        if (mult > best) best = mult;
                    }
                }
            }
        }

        // Temporary boost
        TempBoost tb = tempBoosts.get(uuid);
        long now = System.currentTimeMillis();
        if (tb != null) {
            if (tb.expiresAt <= now) {
                tempBoosts.remove(uuid);
            } else if (tb.multiplier > best) {
                best = tb.multiplier;
            }
        }

        return best;
    }

    public double applyBoost(UUID uuid, double baseAmount, String reason) {
        double mult = getMultiplier(uuid);
        if (mult <= 0) mult = 1.0;
        double boosted = baseAmount * mult;

        if (mult != 1.0) {
            double bonus = boosted - baseAmount;
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            plugin.getTransactionLogger().log(
                    "BOOST",
                    name,
                    bonus,
                    "BOOST_" + reason,
                    true
            );
        }

        return boosted;
    }

    // ─────────────────────────────────────────
    //  Temporary boosts
    // ─────────────────────────────────────────
    public synchronized void setTemporaryBoost(UUID uuid, double multiplier, long durationMillis) {
        TempBoost tb = new TempBoost();
        tb.multiplier = multiplier;
        tb.expiresAt = System.currentTimeMillis() + durationMillis;
        tempBoosts.put(uuid, tb);
    }

    public synchronized void clearTemporaryBoost(UUID uuid) {
        tempBoosts.remove(uuid);
    }

    /**
     * @return remaining millis of temp boost, or 0 if none/expired
     */
    public synchronized long getRemainingMillis(UUID uuid) {
        TempBoost tb = tempBoosts.get(uuid);
        long now = System.currentTimeMillis();
        if (tb == null || tb.expiresAt <= now) {
            tempBoosts.remove(uuid);
            return 0L;
        }
        return tb.expiresAt - now;
    }
}
