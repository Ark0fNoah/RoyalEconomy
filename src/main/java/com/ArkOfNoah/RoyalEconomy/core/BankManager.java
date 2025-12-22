package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    /**
     * Default save (Async)
     */
    public void save() {
        save(true);
    }

    /**
     * Flexible Save Method
     */
    public void save(boolean async) {
        if (bankFile == null) return;

        Map<UUID, Double> balSnapshot = new HashMap<>(bankBalances);
        Map<UUID, Integer> lvlSnapshot = new HashMap<>(bankLevels);

        Runnable saveTask = () -> {
            YamlConfiguration tmpConfig = new YamlConfiguration();

            for (Map.Entry<UUID, Double> entry : balSnapshot.entrySet()) {
                String uuid = entry.getKey().toString();
                tmpConfig.set("accounts." + uuid + ".balance", entry.getValue());
                tmpConfig.set("accounts." + uuid + ".level", lvlSnapshot.getOrDefault(entry.getKey(), 1));
            }

            File tmpFile = new File(plugin.getDataFolder(), "banks.yml.tmp");

            try {
                tmpConfig.save(tmpFile);
                Files.move(tmpFile.toPath(), bankFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save banks.yml!", e);
            }
        };

        if (async) {
            try {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
            } catch (Exception e) {
                saveTask.run();
            }
        } else {
            saveTask.run();
        }
    }

    // --- API Methods ---
    // (Keep your existing API methods below: hasBankAccount, createBankAccount, etc.)
    // Only showing methods that call save() to ensure they are updated

    public boolean hasBankAccount(UUID uuid) {
        return bankBalances.containsKey(uuid);
    }

    public void createBankAccount(UUID uuid) {
        if (!hasBankAccount(uuid)) {
            bankBalances.put(uuid, 0.0);
            bankLevels.put(uuid, 1);
            save(true); // Routine save can be async
        }
    }

    public double getBankBalance(UUID uuid) {
        return bankBalances.getOrDefault(uuid, 0.0);
    }

    public int getBankLevel(UUID uuid) {
        return bankLevels.getOrDefault(uuid, 1);
    }

    public Set<UUID> getBankOwners() {
        return bankBalances.keySet();
    }

    public boolean deposit(UUID uuid, double amount) {
        if (!hasBankAccount(uuid)) return false;
        if (!economyManager.has(uuid, amount)) return false;

        int level = getBankLevel(uuid);
        double limit = plugin.getConfig().getDouble("banks.levels." + level + ".limit", Double.MAX_VALUE);
        if (getBankBalance(uuid) + amount > limit) return false;

        economyManager.withdraw(uuid, amount);
        bankBalances.put(uuid, getBankBalance(uuid) + amount);
        save(true);
        return true;
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!hasBankAccount(uuid)) return false;
        if (getBankBalance(uuid) < amount) return false;

        bankBalances.put(uuid, getBankBalance(uuid) - amount);
        economyManager.deposit(uuid, amount);
        save(true);
        return true;
    }

    public void addInterest(UUID uuid, double amount) {
        if (hasBankAccount(uuid)) {
            bankBalances.put(uuid, getBankBalance(uuid) + amount);
            save(true);
        }
    }
}