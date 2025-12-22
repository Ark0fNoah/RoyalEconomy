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

    // Data Storage
    private final Map<UUID, Double> balances = new HashMap<>();

    // Cached Formatting (Optimization)
    private DecimalFormat moneyFormat;
    private String currencySymbol;
    private String currencyPrefix;
    private String currencySuffix;

    public EconomyManager(RoyalEconomy plugin, StorageHandler storageHandler) {
        this.plugin = plugin;
        this.storageHandler = storageHandler;

        // 1. Load Balances
        this.balances.putAll(storageHandler.getBalances());

        // 2. Initialize Formatters
        reloadCurrencyConfig();
    }

    /**
     * Loads/Reloads currency settings from config.
     * Called on startup and by /royaleconomy reload
     */
    public void reloadCurrencyConfig() {
        String pattern = plugin.getConfig().getString("core.currency.format", "#,##0.00");
        this.moneyFormat = new DecimalFormat(pattern);

        this.currencySymbol = plugin.getConfig().getString("core.currency.symbol", "$");
        this.currencyPrefix = plugin.getConfig().getString("core.currency.prefix", "");
        this.currencySuffix = plugin.getConfig().getString("core.currency.suffix", "");
    }

    // --- Core API Methods ---

    @Override
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, getDefaultBalance());
    }

    @Override
    public void setBalance(UUID uuid, double amount) {
        double max = getMaxBalance();
        if (max > 0 && amount > max) {
            amount = max;
        }
        balances.put(uuid, Math.max(0, amount));
        saveTask();
    }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;

        double current = getBalance(uuid);
        double newBalance = current + amount;

        double max = getMaxBalance();
        if (max > 0 && newBalance > max) {
            return false;
        }

        balances.put(uuid, newBalance);
        saveTask();
        return true;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;

        double current = getBalance(uuid);
        if (current < amount) return false;

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
        if (!has(from, amount)) return false;

        double receiverBal = getBalance(to);
        double max = getMaxBalance();
        if (max > 0 && (receiverBal + amount) > max) {
            return false;
        }

        withdraw(from, amount);
        deposit(to, amount);
        return true;
    }

    @Override
    public String format(double amount) {
        // OPTIMIZATION: Use cached formatter and strings
        // Synchronized because DecimalFormat is not thread-safe
        String formattedNumber;
        synchronized (moneyFormat) {
            formattedNumber = moneyFormat.format(amount);
        }
        return currencyPrefix + currencySymbol + formattedNumber + currencySuffix;
    }

    @Override
    public Map<UUID, Double> getAllBalances() {
        return Collections.unmodifiableMap(balances);
    }

    // --- Helper Methods ---

    public Map<UUID, Double> getAllAccounts() {
        return balances;
    }

    public String getPlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return (op.getName() != null) ? op.getName() : "Unknown";
    }

    private void saveTask() {
        storageHandler.save(balances);
    }

    public double getDefaultBalance() {
        return plugin.getConfig().getDouble("core.starting-balance", 0.0);
    }

    public double getMaxBalance() {
        return plugin.getConfig().getDouble("core.max-balance", -1.0);
    }
}