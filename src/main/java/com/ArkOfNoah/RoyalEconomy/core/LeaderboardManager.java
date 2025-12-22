package com.ArkOfNoah.RoyalEconomy.core;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final EconomyManager economyManager;
    private final int cacheSeconds;

    private List<LeaderboardEntry> cachedTop;
    private long lastUpdate = 0;

    public LeaderboardManager(EconomyManager economyManager, int cacheSeconds) {
        this.economyManager = economyManager;
        this.cacheSeconds = cacheSeconds;
    }

    public List<LeaderboardEntry> getTopAccounts(int limit) {
        long now = System.currentTimeMillis();
        // Check if cache is still valid
        if (cachedTop != null && (now - lastUpdate) < (cacheSeconds * 1000L)) {
            return cachedTop.stream().limit(limit).collect(Collectors.toList());
        }

        // Refresh Cache
        forceRefresh();

        return cachedTop.stream().limit(limit).collect(Collectors.toList());
    }

    public void forceRefresh() {
        // This is a heavy operation, effectively sorting all players
        // In a real database scenario, you would use a SQL query "ORDER BY balance DESC"
        // Since we are using HashMap storage in EconomyManager, we stream it.

        cachedTop = economyManager.getAllAccounts().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue(), economyManager.getPlayerName(entry.getKey())))
                .collect(Collectors.toList());

        lastUpdate = System.currentTimeMillis();
    }

    // --- Inner Class for Data ---
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