package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.entity.Player;

public class RoyalEconomyCommand extends RoyalCommand {

    public RoyalEconomyCommand(RoyalEconomy plugin) {
        super(plugin, EcoRank.ADMIN);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            msgDirect(player, "&bRoyalEconomy &fv" + plugin.getDescription().getVersion());
            msgDirect(player, "&7Created by &fArkOfNoah");
            msgDirect(player, "&7Use &b/royaleconomy reload &7to reload.");
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            long start = System.currentTimeMillis();
            plugin.reloadPlugin(); // Central reload
            long time = System.currentTimeMillis() - start;
            msg(player, "general.reload", "%time%", String.valueOf(time));
            return;
        }

        msg(player, "general.unknown-command");
    }
}