package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {

    private final RoyalEconomy plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(RoyalEconomy plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String get(String key) {
        String prefix = messagesConfig.getString("prefix", "&6Eco &8» &7");
        String msg = messagesConfig.getString(key);

        if (msg == null) return ChatColor.RED + "Missing key: " + key;
        return color(prefix + msg);
    }

    public String getRaw(String key) {
        String msg = messagesConfig.getString(key);
        if (msg == null) return ChatColor.RED + "Missing key: " + key;
        return color(msg);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}