package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final EconomyManager economy;

    public BalanceCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        double balance = economy.getBalance(player.getUniqueId());
        player.sendMessage("§aYour balance: §e" + economy.format(balance));
        return true;
    }
}
