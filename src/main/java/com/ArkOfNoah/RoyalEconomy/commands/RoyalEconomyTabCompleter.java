package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.core.Bank;
import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RoyalEconomyTabCompleter implements TabCompleter {

    private final BankManager bankManager;

    // Pass BankManager so we can autocomplete bank names
    public RoyalEconomyTabCompleter(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        String command = cmd.getName().toLowerCase();
        List<String> suggestions = new ArrayList<>();

        switch (command) {

            case "royaleconomy" -> handleMainCommand(sender, args, suggestions);
            case "pay" -> handlePay(sender, args, suggestions);
            case "eco" -> handleEco(sender, args, suggestions);
            case "baltop" -> handleBaltop(sender, args, suggestions);
            case "bank" -> handleBank(sender, args, suggestions);
        }

        return suggestions;
    }

    // ─────────────────────────────────────────
    // /royaleconomy
    // ─────────────────────────────────────────
    private void handleMainCommand(CommandSender sender, String[] args, List<String> suggestions) {
        if (args.length == 1) {
            if (sender.hasPermission("royaleconomy.reload")) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    suggestions.add("reload");
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // /pay <player> <amount>
    // ─────────────────────────────────────────
    private void handlePay(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("royaleconomy.pay")) return;

        if (args.length == 1) {
            String current = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(current)) {
                    suggestions.add(p.getName());
                }
            }
        }

        if (args.length == 2) {
            String[] amounts = {"10", "50", "100", "250", "500", "1000"};
            for (String s : amounts) {
                if (s.startsWith(args[1].toLowerCase())) {
                    suggestions.add(s);
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // /eco <set|give|take> <player> <amount>
    // ─────────────────────────────────────────
    private void handleEco(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("royaleconomy.admin")) return;

        if (args.length == 1) {
            String[] subs = {"set", "give", "take"};
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) suggestions.add(s);
            }
        }

        if (args.length == 2) {
            String current = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(current)) {
                    suggestions.add(p.getName());
                }
            }
        }

        if (args.length == 3) {
            String[] nums = {"0", "10", "50", "100", "1000"};
            for (String n : nums) {
                if (n.startsWith(args[2].toLowerCase())) suggestions.add(n);
            }
        }
    }

    // ─────────────────────────────────────────
    // /baltop [page]
    // ─────────────────────────────────────────
    private void handleBaltop(CommandSender sender, String[] args, List<String> suggestions) {
        if (args.length == 1) {
            if (!sender.hasPermission("royaleconomy.baltop")) return;
            for (int i = 1; i <= 5; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(args[0].toLowerCase())) {
                    suggestions.add(s);
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // /bank commands
    // ─────────────────────────────────────────
    private void handleBank(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("royaleconomy.bank.use")) return;

        // /bank <action>
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            String[] subs = {
                    "help", "create", "delete", "list", "info",
                    "deposit", "withdraw", "invite", "remove"
            };

            for (String s : subs) {
                if (s.startsWith(current)) suggestions.add(s);
            }
            return;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String current = args[1].toLowerCase();

            // Commands requiring a bank name
            if (sub.matches("delete|info|deposit|withdraw")) {
                for (Bank bank : bankManager.getAllBanks()) {
                    String name = bank.getName();
                    if (name.toLowerCase().startsWith(current)) suggestions.add(name);
                }
                return;
            }

            // Commands requiring a player name
            if (sub.matches("invite|remove")) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().toLowerCase().startsWith(current)) {
                        suggestions.add(op.getName());
                    }
                }
                return;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String current = args[2].toLowerCase();

            // deposit / withdraw amount
            if (sub.equals("deposit") || sub.equals("withdraw")) {
                String[] nums = {"10", "50", "100", "250", "500", "1000"};
                for (String n : nums) {
                    if (n.startsWith(current)) suggestions.add(n);
                }
                return;
            }

            // invite/remove → bank name as 3rd argument
            if (sub.equals("invite") || sub.equals("remove")) {
                for (Bank bank : bankManager.getAllBanks()) {
                    String name = bank.getName();
                    if (name.toLowerCase().startsWith(current)) {
                        suggestions.add(name);
                    }
                }
            }
        }
    }
}
