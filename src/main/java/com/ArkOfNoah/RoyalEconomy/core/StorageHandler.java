package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }
        config.set("balances", null); // clear
        for (Map.Entry<UUID, Double> entry : plugin.getEconomy().getAllBalances().entrySet()) {
            config.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save balances.yml");
            e.printStackTrace();
        }
    }

    public Map<UUID, Double> getLoadedBalances() {
        return loadedBalances;
    }
}
