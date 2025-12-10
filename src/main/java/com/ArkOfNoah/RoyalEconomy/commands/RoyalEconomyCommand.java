package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RoyalEconomyCommand implements CommandExecutor {

    private final RoyalEconomy plugin;

    public RoyalEconomyCommand(RoyalEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§6RoyalEconomy §7v" + plugin.getDescription().getVersion());
            sender.sendMessage("§e/royaleconomy reload §7- Reload config & systems");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("royaleconomy.reload")) {
                sender.sendMessage("§cYou don't have permission to reload RoyalEconomy.");
                return true;
            }

            long start = System.currentTimeMillis();

            plugin.reloadConfig();

            // Reload logger based on new config
            plugin.reloadLogger();

            // Reload banks (if enabled)
            if (plugin.getConfig().getBoolean("banks.enabled", false)) {
                plugin.getBankManager().load();
            }

            // Reset leaderboard cache
            plugin.getLeaderboardManager().forceRefresh();

            sender.sendMessage("§aRoyalEconomy reloaded in §e" +
                    (System.currentTimeMillis() - start) +
                    "ms§a.");

            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
}
