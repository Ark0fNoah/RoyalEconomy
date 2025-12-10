package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {

    private final EconomyManager economy;

    public PayCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /pay <player> <amount>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cThat player has never joined before.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cAmount must be positive.");
            return true;
        }

        if (!economy.transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            sender.sendMessage("§cYou don't have enough money.");
            return true;
        }

        sender.sendMessage("§aYou paid §e" + target.getName() + " " + economy.format(amount));
        if (target.isOnline()) {
            Player t = target.getPlayer();
            t.sendMessage("§aYou received §e" + economy.format(amount) + "§a from §e" + player.getName());
        }
        return true;
    }
}
