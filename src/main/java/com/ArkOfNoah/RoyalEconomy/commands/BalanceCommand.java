package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BalanceCommand extends RoyalCommand {

    public BalanceCommand(RoyalEconomy plugin) {
        super(plugin, EcoRank.PLAYER);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length > 0 && player.hasPermission("royaleconomy.admin")) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                double bal = plugin.getEconomy().getBalance(target.getUniqueId());
                msg(player, "balance.other", "%player%", target.getName(), "%amount%", plugin.getEconomy().format(bal));
                return;
            }
            msg(player, "general.player-not-found", "%player%", args[0]);
            return;
        }

        double bal = plugin.getEconomy().getBalance(player.getUniqueId());
        msg(player, "balance.self", "%amount%", plugin.getEconomy().format(bal));
    }
}