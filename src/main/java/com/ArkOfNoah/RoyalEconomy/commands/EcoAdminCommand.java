package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EcoAdminCommand extends RoyalCommand {

    public EcoAdminCommand(RoyalEconomy plugin) {
        super(plugin, EcoRank.ADMIN);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            msgRaw(player, "admin.usage");
            return;
        }

        String sub = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            msg(player, "general.player-not-found", "%player%", args[1]);
            return;
        }

        // Handle Reset separately as it needs no amount
        if (sub.equals("reset")) {
            plugin.getEconomy().withdraw(target.getUniqueId(), plugin.getEconomy().getBalance(target.getUniqueId()));
            plugin.getEconomy().deposit(target.getUniqueId(), plugin.getConfig().getDouble("starting-balance", 0));
            msg(player, "admin.reset", "%player%", target.getName());
            return;
        }

        if (args.length < 3) {
            msgRaw(player, "admin.usage");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            msg(player, "general.invalid-number");
            return;
        }

        String fmtAmount = plugin.getEconomy().format(amount);

        switch (sub) {
            case "give":
                plugin.getEconomy().deposit(target.getUniqueId(), amount);
                msg(player, "admin.give", "%amount%", fmtAmount, "%player%", target.getName(), "%total%", plugin.getEconomy().format(plugin.getEconomy().getBalance(target.getUniqueId())));
                break;
            case "take":
                plugin.getEconomy().withdraw(target.getUniqueId(), amount);
                msg(player, "admin.take", "%amount%", fmtAmount, "%player%", target.getName(), "%total%", plugin.getEconomy().format(plugin.getEconomy().getBalance(target.getUniqueId())));
                break;
            case "set":
                double current = plugin.getEconomy().getBalance(target.getUniqueId());
                plugin.getEconomy().withdraw(target.getUniqueId(), current);
                plugin.getEconomy().deposit(target.getUniqueId(), amount);
                msg(player, "admin.set", "%amount%", fmtAmount, "%player%", target.getName());
                break;
            default:
                msgRaw(player, "admin.usage");
        }
    }
}