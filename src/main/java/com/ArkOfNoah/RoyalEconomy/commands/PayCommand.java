package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {

    private final RoyalEconomy plugin;
    private final EconomyManager economy;
    private final FileConfiguration config;

    public PayCommand(RoyalEconomy plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.player-only",
                    "%prefix% &cOnly players can use this command."
            ))));
            return true;
        }

        if (!sender.hasPermission("royaleconomy.pay")) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.no-permission",
                    "%prefix% &cYou don't have permission to do that."
            ))));
            return true;
        }

        // Enabled switch in config
        if (!config.getBoolean("core.commands.pay.enabled", true)) {
            sender.sendMessage(color(applyPrefix("&c/pay is disabled.")));
            return true;
        }

        if (args.length != 2) {
            String usage = config.getString(
                    "core.core-messages.pay-usage",
                    "%prefix% &cUsage: /pay <player> <amount>"
            );
            sender.sendMessage(color(applyPrefix(usage)));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        boolean requireOnline = config.getBoolean("core.commands.pay.require-online-target", false);
        if (requireOnline && !target.isOnline()) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.target-not-found",
                    "%prefix% &cThat player is not online."
            ))));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.invalid-amount",
                    "%prefix% &cThat is not a valid amount."
            ))));
            return true;
        }

        double minAmount = config.getDouble("core.commands.pay.min-amount", 0.01);
        if (amount <= 0 || amount < minAmount) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.negative-amount",
                    "%prefix% &cAmount must be at least " + minAmount + "."
            ))));
            return true;
        }

        double maxAmount = config.getDouble("core.commands.pay.max-amount", -1);
        if (maxAmount > 0 && amount > maxAmount) {
            sender.sendMessage(color(applyPrefix("&cYou cannot pay more than " + maxAmount + " at once.")));
            return true;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(color(applyPrefix("&cYou cannot pay yourself.")));
            return true;
        }

        // Withdraw from sender
        if (!economy.withdraw(player.getUniqueId(), amount)) {
            String msg = config.getString(
                    "core.core-messages.insufficient-funds",
                    "%prefix% &cYou don't have enough money."
            );
            sender.sendMessage(color(applyPrefix(msg)));
            plugin.getTransactionLogger().log(
                    player.getName(),
                    target.getName(),
                    amount,
                    "PAY_FAIL_INSUFFICIENT",
                    false
            );
            return true;
        }

        // Deposit to target (check max balance)
        double targetBalance = economy.getBalance(target.getUniqueId());
        double maxBalance = config.getDouble("core.max-balance", 1000000000.0);
        if (maxBalance > 0 && !sender.hasPermission("royaleconomy.bypass.maxbalance")) {
            if (targetBalance + amount > maxBalance) {
                // rollback sender
                economy.deposit(player.getUniqueId(), amount);

                String msg = config.getString(
                        "core.core-messages.max-balance-reached",
                        "%prefix% &cThat player cannot receive more money (max balance reached)."
                );
                sender.sendMessage(color(applyPrefix(msg)));
                plugin.getTransactionLogger().log(
                        player.getName(),
                        target.getName(),
                        amount,
                        "PAY_FAIL_TARGET_MAX_BALANCE",
                        false
                );
                return true;
            }
        }

        // Apply tax on /pay
        double netAmount = plugin.getTaxManager().applyTax(
                "pay",
                amount,
                player.getUniqueId(),
                target.getUniqueId()
        );

        // If tax ate everything, skip deposit
        if (netAmount > 0) {
            economy.deposit(target.getUniqueId(), netAmount);
        }

        // Messages
        String sentMsg = config.getString(
                "core.core-messages.pay-sent",
                "%prefix% &aYou paid &e%target% %amount_formatted%&a."
        );
        sentMsg = sentMsg
                .replace("%target%", target.getName() != null ? target.getName() : target.getUniqueId().toString())
                .replace("%amount_formatted%", economy.format(netAmount));
        player.sendMessage(color(applyPrefix(sentMsg)));

        if (target.isOnline()) {
            String receivedMsg = config.getString(
                    "core.core-messages.pay-received",
                    "%prefix% &aYou received %amount_formatted% &afrom &e%player%&a."
            );
            receivedMsg = receivedMsg
                    .replace("%player%", player.getName())
                    .replace("%amount_formatted%", economy.format(amount));
            target.getPlayer().sendMessage(color(applyPrefix(receivedMsg)));
        }

        // Logging
        plugin.getTransactionLogger().log(
                player.getName(),
                target.getName(),
                amount,
                "PAY",
                true
        );

        return true;
    }

    private String getPrefix() {
        if (!config.getBoolean("messages.use-prefix", true)) return "";
        return config.getString("messages.prefix", "&8[&6RoyalEconomy&8]&r ");
    }

    private String applyPrefix(String msg) {
        return msg == null ? "" : msg.replace("%prefix%", getPrefix());
    }

    private String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
