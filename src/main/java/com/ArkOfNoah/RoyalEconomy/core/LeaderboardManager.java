package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.api.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    public void forceRefresh() {
        this.lastUpdate = 0;
    }

    public static class Entry {
        private final UUID uuid;
        private final double balance;

        public Entry(UUID uuid, double balance) {
            this.uuid = uuid;
            this.balance = balance;
        }

        public UUID getUuid() {
            return uuid;
        }

        public double getBalance() {
            return balance;
        }

        public String getName() {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            return op.getName() != null ? op.getName() : uuid.toString();
        }
    }

    private final Economy economy;
    private final int cacheSeconds;

    private List<Entry> cached = new ArrayList<>();
    private long lastUpdate = 0L;

    public LeaderboardManager(Economy economy, int cacheSeconds) {
        this.economy = economy;
        this.cacheSeconds = cacheSeconds;
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < cacheSeconds * 1000L) {
            return;
        }
        lastUpdate = now;

        Map<UUID, Double> all = economy.getAllBalances();
        cached = all.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Entry::getBalance).reversed())
                .collect(Collectors.toList());
    }

    public List<Entry> getPage(int page, int pageSize) {
        refreshIfNeeded();
        if (page <= 0) page = 1;
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, cached.size());
        if (from >= cached.size()) return Collections.emptyList();
        return cached.subList(from, to);
    }

    public int getTotalPages(int pageSize) {
        refreshIfNeeded();
        if (pageSize <= 0) return 1;
        return (int) Math.max(1, Math.ceil(cached.size() / (double) pageSize));
    }

    public int getRank(UUID uuid) {
        refreshIfNeeded();
        for (int i = 0; i < cached.size(); i++) {
            if (cached.get(i).getUuid().equals(uuid)) {
                return i + 1; // 1-based
            }
        }
        return -1;
    }
}
