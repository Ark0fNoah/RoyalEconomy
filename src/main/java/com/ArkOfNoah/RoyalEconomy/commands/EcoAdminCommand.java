package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

public class EcoAdminCommand implements CommandExecutor {

    private final EconomyManager economy;

    public EcoAdminCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("RoyalEconomy.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length != 3) {
            sender.sendMessage("§cUsage: /eco <set|give|take> <player> <amount>");
            return true;
        }

        String sub = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount;

        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount.");
            return true;
        }

        switch (sub) {
            case "set" -> {
                economy.setBalance(target.getUniqueId(), amount);
                sender.sendMessage("§aSet " + target.getName() + "'s balance to " + economy.format(amount));
            }
            case "give" -> {
                economy.deposit(target.getUniqueId(), amount);
                sender.sendMessage("§aGave " + economy.format(amount) + " to " + target.getName());
            }
            case "take" -> {
                if (!economy.withdraw(target.getUniqueId(), amount)) {
                    sender.sendMessage("§cPlayer doesn't have enough money.");
                    return true;
                }
                sender.sendMessage("§aTook " + economy.format(amount) + " from " + target.getName());
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use set, give, or take.");
        }
        return true;
    }
}
