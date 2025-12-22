package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager implements Economy {

    private final RoyalEconomy plugin;
    private final StorageHandler storageHandler;
    // This is the single source of truth for balances
    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(RoyalEconomy plugin, StorageHandler storageHandler) {
        this.plugin = plugin;
        this.storageHandler = storageHandler;

        // Load data from storage into RAM immediately
        // FIX: changed 'accounts' to 'balances'
        this.balances.putAll(storageHandler.getBalances());
    }

    // --- Core API Methods ---

    @Override
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, getDefaultBalance());
    }

    @Override
    public void setBalance(UUID uuid, double amount) {
        // Enforce max balance check even on set
        double max = getMaxBalance();
        if (max > 0 && amount > max) {
            amount = max;
        }
        balances.put(uuid, Math.max(0, amount));
        saveTask(); // Auto-save logic
    }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;

        double current = getBalance(uuid);
        double newBalance = current + amount;

        // Check Max Balance Config
        double max = getMaxBalance();
        if (max > 0 && newBalance > max) {
            return false; // Transaction failed: would exceed limit
        }

        balances.put(uuid, newBalance);
        saveTask();
        return true;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;

        double current = getBalance(uuid);
        if (current < amount) return false; // Insufficient funds

        balances.put(uuid, current - amount);
        saveTask();
        return true;
    }

    @Override
    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    @Override
    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return false;

        // 1. Check if sender has enough
        if (!has(from, amount)) return false;

        // 2. Check if receiver can hold that much (Max Balance)
        double receiverBal = getBalance(to);
        double max = getMaxBalance();
        if (max > 0 && (receiverBal + amount) > max) {
            return false; // Transfer failed: receiver is full
        }

        // 3. Execute (saveTask is called inside withdraw/deposit)
        withdraw(from, amount);
        deposit(to, amount);
        return true;
    }

    @Override
    public String format(double amount) {
        // Pull format settings directly from config.yml
        String pattern = plugin.getConfig().getString("core.currency.format", "#,##0.00");
        String symbol = plugin.getConfig().getString("core.currency.symbol", "$");
        String prefix = plugin.getConfig().getString("core.currency.prefix", "");
        String suffix = plugin.getConfig().getString("core.currency.suffix", "");

        DecimalFormat df = new DecimalFormat(pattern);
        String formattedNumber = df.format(amount);

        // Result example: "$1,250.00"
        return prefix + symbol + formattedNumber + suffix;
    }

    @Override
    public Map<UUID, Double> getAllBalances() {
        return Collections.unmodifiableMap(balances);
    }

    // --- Helper Methods ---

    /**
     * Used by LeaderboardManager to get the raw map
     */
    public Map<UUID, Double> getAllAccounts() {
        return balances;
    }

    public String getPlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return (op.getName() != null) ? op.getName() : "Unknown";
    }

    private void saveTask() {
        // We push the RAM data back to the storage handler
        // FIX: changed 'accounts' to 'balances'
        storageHandler.save(balances);
    }

    public double getDefaultBalance() {
        return plugin.getConfig().getDouble("core.starting-balance", 0.0);
    }

    public double getMaxBalance() {
        return plugin.getConfig().getDouble("core.max-balance", -1.0);
    }
}