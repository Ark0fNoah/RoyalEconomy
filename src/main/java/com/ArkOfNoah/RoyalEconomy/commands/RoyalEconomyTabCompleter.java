package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoyalEconomyTabCompleter implements TabCompleter {

    private final BankManager bankManager;

    public RoyalEconomyTabCompleter(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // Return empty list if not a player (optional, but usually safer)
        if (!(sender instanceof Player)) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();
        String commandName = cmd.getName().toLowerCase();

        // Match command to handler
        if (commandName.equals("royaleconomy")) {
            handleMainCommand(sender, args, suggestions);
        } else if (commandName.equals("pay")) {
            handlePay(sender, args, suggestions);
        } else if (commandName.equals("eco")) {
            handleEco(sender, args, suggestions);
        } else if (commandName.equals("baltop")) {
            handleBaltop(sender, args, suggestions);
        } else if (commandName.equals("bank")) {
            handleBank(sender, args, suggestions);
        }

        return suggestions;
    }

    // ─────────────────────────────────────────
    // /royaleconomy <reload>
    // ─────────────────────────────────────────
    private void handleMainCommand(CommandSender sender, String[] args, List<String> suggestions) {
        if (args.length == 1) {
            if (sender.hasPermission("royaleconomy.admin")) {
                StringUtil.copyPartialMatches(args[0], Collections.singletonList("reload"), suggestions);
            }
        }
    }

    // ─────────────────────────────────────────
    // /pay <player> <amount>
    // ─────────────────────────────────────────
    private void handlePay(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("royaleconomy.pay")) return;

        if (args.length == 1) {
            // Suggest online players
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[0], playerNames, suggestions);
        }
        else if (args.length == 2) {
            // Suggest common amounts
            List<String> amounts = Arrays.asList("10", "100", "500", "1000");
            StringUtil.copyPartialMatches(args[1], amounts, suggestions);
        }
    }

    // ─────────────────────────────────────────
    // /eco <give|take|set|reset> <player> <amount>
    // ─────────────────────────────────────────
    private void handleEco(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("royaleconomy.admin")) return;

        if (args.length == 1) {
            List<String> subs = Arrays.asList("give", "take", "set", "reset");
            StringUtil.copyPartialMatches(args[0], subs, suggestions);
        }
        else if (args.length == 2) {
            // Suggest players for the second argument
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, suggestions);
        }
        else if (args.length == 3) {
            // Suggest amounts (except for reset)
            if (!args[0].equalsIgnoreCase("reset")) {
                List<String> amounts = Arrays.asList("100", "1000", "5000", "10000");
                StringUtil.copyPartialMatches(args[2], amounts, suggestions);
            }
        }
    }

    // ─────────────────────────────────────────
    // /baltop [page]
    // ─────────────────────────────────────────
    private void handleBaltop(CommandSender sender, String[] args, List<String> suggestions) {
        if (args.length == 1) {
            List<String> pages = Arrays.asList("1", "2", "3", "4", "5");
            StringUtil.copyPartialMatches(args[0], pages, suggestions);
        }
    }

    // ─────────────────────────────────────────
    // /bank <create|info|deposit|withdraw> [amount]
    // ─────────────────────────────────────────
    private void handleBank(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("royaleconomy.bank.use")) return;

        if (args.length == 1) {
            List<String> subs = Arrays.asList("create", "info", "deposit", "withdraw");
            StringUtil.copyPartialMatches(args[0], subs, suggestions);
        }
        else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            // Only suggest amounts for deposit/withdraw
            if (sub.equals("deposit") || sub.equals("withdraw")) {
                List<String> amounts = Arrays.asList("100", "500", "1000", "5000");
                StringUtil.copyPartialMatches(args[1], amounts, suggestions);
            }
        }
    }
}