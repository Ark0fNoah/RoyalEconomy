package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class RoyalCommand implements CommandExecutor {

    protected final RoyalEconomy plugin;
    private final EcoRank requiredRank;

    public RoyalCommand(RoyalEconomy plugin, EcoRank requiredRank) {
        this.plugin = plugin;
        this.requiredRank = requiredRank;
    }

    public abstract void execute(Player player, String[] args);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().get("general.only-players"));
            return true;
        }

        Player player = (Player) sender;

        if (requiredRank == EcoRank.ADMIN && !player.hasPermission("royaleconomy.admin")) {
            msg(player, "general.no-permission");
            return true;
        }

        execute(player, args);
        return true;
    }

    // --- New Message Logic ---

    protected void msg(Player player, String key, String... placeholders) {
        String text = plugin.getMessageManager().get(key);
        text = applyPlaceholders(text, placeholders);
        player.sendMessage(text);
    }

    // For raw messages without prefix (like usage)
    protected void msgRaw(Player player, String key, String... placeholders) {
        String text = plugin.getMessageManager().getRaw(key);
        text = applyPlaceholders(text, placeholders);
        player.sendMessage(text);
    }

    // For direct strings (rarely used now, but good for debug)
    protected void msgDirect(Player player, String text) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }

    private String applyPlaceholders(String text, String... placeholders) {
        if (placeholders.length == 0) return text;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            if (value != null) {
                text = text.replace(placeholder, value);
            }
        }
        return text;
    }
}