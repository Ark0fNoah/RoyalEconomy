package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BankStorage {

    private final RoyalEconomy plugin;
    private final File file;
    private FileConfiguration config;

    public BankStorage(RoyalEconomy plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "banks.yml");
    }

    public void load(Map<String, Bank> targetMap) {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create banks.yml");
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        targetMap.clear();

        ConfigurationSection banksSec = config.getConfigurationSection("banks");
        if (banksSec == null) return;

        for (String idStr : banksSec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String path = "banks." + idStr + ".";
                String name = config.getString(path + "name");
                UUID owner = UUID.fromString(config.getString(path + "owner"));

                double balance = config.getDouble(path + "balance");
                int maxMembers = config.getInt(path + "maxMembers");

                List<String> memberStr = config.getStringList(path + "members");
                Set<UUID> members = new HashSet<>();
                for (String s : memberStr) {
                    try {
                        members.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) { }
                }

                if (name == null || owner == null) continue;

                Bank bank = new Bank(id, name, owner, balance, maxMembers, members);
                targetMap.put(name.toLowerCase(), bank);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in banks.yml: " + idStr);
            }
        }
    }

    public void save(Map<String, Bank> sourceMap) {
        if (config == null) {
            config = new YamlConfiguration();
        }
        config.set("banks", null);

        for (Bank bank : sourceMap.values()) {
            String idStr = bank.getId().toString();
            String path = "banks." + idStr + ".";
            config.set(path + "name", bank.getName());
            config.set(path + "owner", bank.getOwner().toString());
            config.set(path + "balance", bank.getBalance());
            config.set(path + "maxMembers", bank.getMaxMembers());

            List<String> members = new ArrayList<>();
            for (UUID u : bank.getMembers()) {
                members.add(u.toString());
            }
            config.set(path + "members", members);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save banks.yml");
            e.printStackTrace();
        }
    }
}
