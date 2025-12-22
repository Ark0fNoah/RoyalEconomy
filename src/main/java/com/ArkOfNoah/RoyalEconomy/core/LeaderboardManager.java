package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final RoyalEconomy plugin;
    private final EconomyManager economyManager;
    private final int cacheSeconds;

    // Cache the result so we don't recalculate constantly
    private List<LeaderboardEntry> cachedTop = new ArrayList<>();
    private long lastUpdate = 0;

    // Prevent multiple background tasks running at once
    private boolean isUpdating = false;

    public LeaderboardManager(RoyalEconomy plugin, EconomyManager economyManager, int cacheSeconds) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.cacheSeconds = cacheSeconds;
    }

    /**
     * Returns the cached leaderboard. Triggers an update if expired.
     */
    public List<LeaderboardEntry> getTopAccounts(int limit) {
        long now = System.currentTimeMillis();

        // If cache expired and we aren't already updating, trigger a background refresh
        if ((now - lastUpdate) > (cacheSeconds * 1000L) && !isUpdating) {
            refreshAsync();
        }

        // Return current cache (safe against index errors)
        if (cachedTop.isEmpty()) return Collections.emptyList();
        return cachedTop.subList(0, Math.min(limit, cachedTop.size()));
    }

    /**
     * Runs the heavy sorting on a background thread
     */
    public void refreshAsync() {
        isUpdating = true;

        // 1. Snapshot the data on the Main Thread
        // This prevents "ConcurrentModificationException" if players pay/trade during the sort.
        Map<UUID, Double> snapshot = new HashMap<>(economyManager.getAllBalances());

        // 2. Run Sorting & Name Lookup Asynchronously (Off the main thread)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            List<LeaderboardEntry> sorted = snapshot.entrySet().stream()
                    // Sort by Balance (Highest to Lowest)
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                    // Optimization: Only keep top 100 ever. No need to sort 10,000th player.
                    .limit(100)
                    .map(entry -> {
                        // Lookup name (Safe to do async for OfflinePlayer usually)
                        String name = economyManager.getPlayerName(entry.getKey());
                        return new LeaderboardEntry(entry.getKey(), entry.getValue(), name);
                    })
                    .collect(Collectors.toList());

            // 3. Update Cache back on the Main Thread (Thread Safety)
            Bukkit.getScheduler().runTask(plugin, () -> {
                cachedTop = sorted;
                lastUpdate = System.currentTimeMillis();
                isUpdating = false;
            });
        });
    }

    public void forceRefresh() {
        refreshAsync();
    }

    // --- Data Class ---
    public static class LeaderboardEntry {
        private final UUID uuid;
        private final double balance;
        private final String name;

        public LeaderboardEntry(UUID uuid, double balance, String name) {
            this.uuid = uuid;
            this.balance = balance;
            this.name = (name != null) ? name : "Unknown";
        }

        public String getName() { return name; }
        public double getBalance() { return balance; }
    }
}