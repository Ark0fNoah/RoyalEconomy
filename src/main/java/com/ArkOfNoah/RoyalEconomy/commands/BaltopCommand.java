package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.LeaderboardManager;
import com.ArkOfNoah.RoyalEconomy.core.LeaderboardManager.Entry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class BaltopCommand implements CommandExecutor {

    private final RoyalEconomy plugin;
    private final LeaderboardManager leaderboard;
    private final FileConfiguration config;

    public BaltopCommand(RoyalEconomy plugin, LeaderboardManager leaderboard) {
        this.plugin = plugin;
        this.leaderboard = leaderboard;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!config.getBoolean("baltop.enabled", true)) {
            sender.sendMessage(color(applyPrefix("&cBaltop is disabled.")));
            return true;
        }

        if (!sender.hasPermission("royaleconomy.baltop")) {
            sender.sendMessage(color(applyPrefix(config.getString("messages.no-permission",
                    "&cNo permission."))));
            return true;
        }

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) { }
        }

        int pageSize = config.getInt("baltop.page-size", 10);
        List<Entry> entries = leaderboard.getPage(page, pageSize);
        int totalPages = leaderboard.getTotalPages(pageSize);

        if (entries.isEmpty()) {
            sender.sendMessage(color(applyPrefix(config.getString("baltop.messages.no-data",
                    "&7No balance data available."))));
            return true;
        }

        String header = config.getString("baltop.messages.header",
                "%prefix% &6Top balances (&ePage %page%/%pages%&6)");
        header = header
                .replace("%prefix%", getPrefix())
                .replace("%page%", String.valueOf(page))
                .replace("%pages%", String.valueOf(totalPages))
                .replace("%page_size%", String.valueOf(pageSize));

        sender.sendMessage(color(header));

        int positionStart = (page - 1) * pageSize + 1;
        String entryFormat = config.getString("baltop.messages.entry",
                "&7%position%. &e%player% &7- &a%balance_formatted%");

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int pos = positionStart + i;
            String line = entryFormat
                    .replace("%position%", String.valueOf(pos))
                    .replace("%player%", e.getName())
                    .replace("%balance%", String.valueOf(e.getBalance()))
                    .replace("%balance_formatted%", plugin.getEconomy().format(e.getBalance()))
                    .replace("%prefix%", getPrefix());
            sender.sendMessage(color(line));
        }

        // Optional self rank
        if (config.getBoolean("baltop.show-self-rank", true) && sender instanceof Player player) {
            int rank = leaderboard.getRank(player.getUniqueId());
            if (rank > 0) {
                String fmt = config.getString("baltop.messages.self-rank",
                        "%prefix% &7You are &e#%rank% &7with &a%balance_formatted%");
                fmt = fmt
                        .replace("%prefix%", getPrefix())
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%balance_formatted%",
                                plugin.getEconomy().format(plugin.getEconomy().getBalance(player.getUniqueId())));
                sender.sendMessage(color(fmt));
            }
        }

        return true;
    }

    private String getPrefix() {
        if (!config.getBoolean("messages.use-prefix", true)) return "";
        return config.getString("messages.prefix", "&8[&6RoyalEconomy&8]&r ");
    }

    private String applyPrefix(String msg) {
        return msg.replace("%prefix%", getPrefix());
    }

    private String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
