package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class EcoAdminCommand implements CommandExecutor {

    private final RoyalEconomy plugin;
    private final EconomyManager economy;
    private final FileConfiguration config;

    public EcoAdminCommand(RoyalEconomy plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("royaleconomy.admin")) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.no-permission",
                    "%prefix% &cYou don't have permission to do that."
            ))));
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(color(applyPrefix("&cUsage: /eco <set|give|take> <player> <amount>")));
            return true;
        }

        String sub = args[0].toLowerCase();
        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.invalid-amount",
                    "%prefix% &cThat is not a valid amount."
            ))));
            return true;
        }

        if (amount < 0) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.negative-amount",
                    "%prefix% &cAmount must be positive."
            ))));
            return true;
        }

        String senderName = sender.getName();

        switch (sub) {
            case "set" -> {
                economy.setBalance(target.getUniqueId(), amount);

                String msg = "&aSet " + targetName + "'s balance to " + economy.format(amount);
                sender.sendMessage(color(applyPrefix(msg)));

                plugin.getTransactionLogger().log(
                        senderName,
                        targetName,
                        amount,
                        "COMMAND_SET",
                        true
                );
            }

            case "give" -> {
                economy.deposit(target.getUniqueId(), amount);

                String msg = "&aGave " + economy.format(amount) + " to " + targetName;
                sender.sendMessage(color(applyPrefix(msg)));

                plugin.getTransactionLogger().log(
                        senderName,
                        targetName,
                        amount,
                        "COMMAND_GIVE",
                        true
                );
            }

            case "take" -> {
                boolean success = economy.withdraw(target.getUniqueId(), amount);
                if (!success) {
                    sender.sendMessage(color(applyPrefix(config.getString(
                            "core.core-messages.insufficient-funds",
                            "%prefix% &cPlayer doesn't have enough money."
                    ))));
                    plugin.getTransactionLogger().log(
                            senderName,
                            targetName,
                            amount,
                            "COMMAND_TAKE_FAIL_INSUFFICIENT",
                            false
                    );
                    return true;
                }

                String msg = "&aTook " + economy.format(amount) + " from " + targetName;
                sender.sendMessage(color(applyPrefix(msg)));

                plugin.getTransactionLogger().log(
                        senderName,
                        targetName,
                        amount,
                        "COMMAND_TAKE",
                        true
                );
            }

            default -> {
                sender.sendMessage(color(applyPrefix(config.getString(
                        "messages.unknown-command",
                        "%prefix% &cUnknown subcommand."
                ))));
            }
        }

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
