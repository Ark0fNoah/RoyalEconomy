package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.LeaderboardManager;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.entity.Player;
import java.util.List;

public class BaltopCommand extends RoyalCommand {

    public BaltopCommand(RoyalEconomy plugin) {
        super(plugin, EcoRank.PLAYER);
    }

    @Override
    public void execute(Player player, String[] args) {
        List<LeaderboardManager.LeaderboardEntry> top = plugin.getLeaderboardManager().getTopAccounts(10);

        msgRaw(player, "baltop.header");

        int rank = 1;
        for (LeaderboardManager.LeaderboardEntry entry : top) {
            msgRaw(player, "baltop.entry",
                    "%rank%", String.valueOf(rank),
                    "%player%", entry.getName(),
                    "%amount%", plugin.getEconomy().format(entry.getBalance())
            );
            rank++;
        }

        msgRaw(player, "baltop.footer");
    }
}