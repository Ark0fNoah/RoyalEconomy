package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StorageHandler {

    private final RoyalEconomy plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<UUID, Double> loadedBalances = new HashMap<>();

    public StorageHandler(RoyalEconomy plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create balances.yml");
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        loadedBalances.clear();

        if (config.isConfigurationSection("balances")) {
            for (String key : config.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double balance = config.getDouble("balances." + key);
                    loadedBalances.put(uuid, balance);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in balances.yml: " + key);
                }
            }
        }
    }

    /**
     * Default save (Async) - Used by auto-save tasks
     */
    public void save(Map<UUID, Double> currentBalances) {
        save(currentBalances, true);
    }

    /**
     * Flexible Save Method
     * @param async If true, runs in background (prevent lag). If false, runs immediately (safe for onDisable).
     */
    public void save(Map<UUID, Double> currentBalances, boolean async) {
        // 1. Snapshot on Main Thread
        Map<UUID, Double> snapshot = new HashMap<>(currentBalances);

        // Define the saving logic
        Runnable saveTask = () -> {
            YamlConfiguration tmpConfig = new YamlConfiguration();

            for (Map.Entry<UUID, Double> entry : snapshot.entrySet()) {
                tmpConfig.set("balances." + entry.getKey().toString(), entry.getValue());
            }

            File tmpFile = new File(plugin.getDataFolder(), "balances.yml.tmp");

            try {
                tmpConfig.save(tmpFile);
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save balances.yml!", e);
            }
        };

        // 2. Execute
        if (async) {
            try {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
            } catch (Exception e) {
                // If async fails (rare edge case), run sync
                saveTask.run();
            }
        } else {
            saveTask.run(); // Run immediately on main thread
        }
    }

    public Map<UUID, Double> getBalances() {
        return loadedBalances;
    }
}