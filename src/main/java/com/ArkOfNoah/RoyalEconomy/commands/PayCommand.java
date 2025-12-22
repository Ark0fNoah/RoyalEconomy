package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PayCommand extends RoyalCommand {

    public PayCommand(RoyalEconomy plugin) {
        super(plugin, EcoRank.PLAYER);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            msgRaw(player, "pay.usage");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            msg(player, "general.player-not-found", "%player%", args[0]);
            return;
        }

        if (target.equals(player)) {
            msg(player, "pay.self-pay");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            msg(player, "general.invalid-number");
            return;
        }

        if (!plugin.getEconomy().has(player.getUniqueId(), amount)) {
            double bal = plugin.getEconomy().getBalance(player.getUniqueId());
            msg(player, "pay.insufficient-funds", "%balance%", plugin.getEconomy().format(bal), "%amount%", plugin.getEconomy().format(amount));
            return;
        }

        // Execute Transaction
        plugin.getEconomy().withdraw(player.getUniqueId(), amount);
        plugin.getEconomy().deposit(target.getUniqueId(), amount);

        String fmtAmount = plugin.getEconomy().format(amount);

        // Messages
        msg(player, "pay.sent", "%amount%", fmtAmount, "%target%", target.getName());
        msg(target, "pay.received", "%amount%", fmtAmount, "%sender%", player.getName());

        // Log
        plugin.getTransactionLogger().logTransaction(player.getName(), target.getName(), amount, "PAY_COMMAND");
    }
}