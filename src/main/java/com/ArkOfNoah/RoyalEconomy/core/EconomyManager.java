package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomyPlugin;
import com.ArkOfNoah.RoyalEconomy.api.Economy;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager implements Economy {

    private final RoyalEconomyPlugin plugin;
    private final StorageHandler storage;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public EconomyManager(RoyalEconomyPlugin plugin, StorageHandler storage) {
        this.plugin = plugin;
        this.storage = storage;

        // preload from storage
        balances.putAll(storage.getLoadedBalances());
    }

    @Override
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, getDefaultBalance());
    }

    @Override
    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;
        double current = getBalance(uuid);
        balances.put(uuid, current + amount);
        return true;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        double current = getBalance(uuid);
        if (current < amount) return false;
        balances.put(uuid, current - amount);
        return true;
    }

    @Override
    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    @Override
    public boolean transfer(UUID from, UUID to, double amount) {
        if (!withdraw(from, amount)) return false;
        if (!deposit(to, amount)) {
            // rollback if deposit fails (just in case)
            deposit(from, amount);
            return false;
        }
        return true;
    }

    @Override
    public String format(double amount) {
        // You can make this configurable via config.yml
        return decimalFormat.format(amount) + " coins";
    }

    public double getDefaultBalance() {
        return plugin.getConfig().getDouble("default-balance", 0.0);
    }

    public Map<UUID, Double> getAllBalances() {
        return balances;
    }
}
