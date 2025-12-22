package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BankManager {

    private final RoyalEconomy plugin;
    private final EconomyManager economyManager;
    private final Map<UUID, Double> bankBalances = new HashMap<>();
    private final Map<UUID, Integer> bankLevels = new HashMap<>();
    private File bankFile;
    private YamlConfiguration bankConfig;

    public BankManager(RoyalEconomy plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    public void load() {
        bankFile = new File(plugin.getDataFolder(), "banks.yml");

        // FIX: Create an empty file instead of trying to copy one from the JAR
        if (!bankFile.exists()) {
            try {
                bankFile.getParentFile().mkdirs();
                bankFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create banks.yml!");
                e.printStackTrace();
            }
        }

        bankConfig = YamlConfiguration.loadConfiguration(bankFile);

        bankBalances.clear();
        bankLevels.clear();

        ConfigurationSection section = bankConfig.getConfigurationSection("accounts");
        if (section != null) {
            for (String uuidStr : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    bankBalances.put(uuid, section.getDouble(uuidStr + ".balance"));
                    bankLevels.put(uuid, section.getInt(uuidStr + ".level", 1));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in banks.yml: " + uuidStr);
                }
            }
        }
    }

    public void save() {
        if (bankConfig == null || bankFile == null) return;

        for (Map.Entry<UUID, Double> entry : bankBalances.entrySet()) {
            String path = "accounts." + entry.getKey().toString();
            bankConfig.set(path + ".balance", entry.getValue());
            bankConfig.set(path + ".level", bankLevels.getOrDefault(entry.getKey(), 1));
        }

        try {
            bankConfig.save(bankFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save banks.yml!", e);
        }
    }

    // --- API Methods Used by Commands ---

    public boolean hasBankAccount(UUID uuid) {
        return bankBalances.containsKey(uuid);
    }

    public java.util.Set<UUID> getBankOwners() {
        return bankBalances.keySet();
    }

    public void createBankAccount(UUID uuid) {
        if (!hasBankAccount(uuid)) {
            bankBalances.put(uuid, 0.0);
            bankLevels.put(uuid, 1);
            save(); // Save immediately to prevent data loss
        }
    }

    public double getBankBalance(UUID uuid) {
        return bankBalances.getOrDefault(uuid, 0.0);
    }

    public int getBankLevel(UUID uuid) {
        return bankLevels.getOrDefault(uuid, 1);
    }

    public boolean deposit(UUID uuid, double amount) {
        if (!hasBankAccount(uuid)) return false;

        // Check wallet
        if (!economyManager.has(uuid, amount)) return false;

        // Check Bank Limit (Optional logic)
        int level = getBankLevel(uuid);
        double limit = plugin.getConfig().getDouble("banks.levels." + level + ".limit", Double.MAX_VALUE);
        if (getBankBalance(uuid) + amount > limit) return false;

        // Execute
        economyManager.withdraw(uuid, amount);
        bankBalances.put(uuid, getBankBalance(uuid) + amount);
        return true;
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!hasBankAccount(uuid)) return false;
        if (getBankBalance(uuid) < amount) return false;

        bankBalances.put(uuid, getBankBalance(uuid) - amount);
        economyManager.deposit(uuid, amount);
        return true;
    }

    // Used by InterestTask
    public void addInterest(UUID uuid, double amount) {
        if (hasBankAccount(uuid)) {
            bankBalances.put(uuid, getBankBalance(uuid) + amount);
        }
    }
}