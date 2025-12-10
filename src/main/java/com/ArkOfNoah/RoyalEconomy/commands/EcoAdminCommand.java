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

        // /eco debugtest
        if (args.length >= 1 && args[0].equalsIgnoreCase("debugtest")) {
            runDebugTest(sender);
            return true;
        }

        // Normal /eco <set|give|take> <player> <amount>
        if (args.length != 3) {
            sender.sendMessage(color(applyPrefix("&cUsage: /eco <set|give|take> <player> <amount>")));
            sender.sendMessage(color(applyPrefix("&7Or: &e/eco debugtest &7to run a quick diagnostic.")));
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

    // ─────────────────────────────────────────
    // /eco debugtest implementation
    // ─────────────────────────────────────────
    private void runDebugTest(CommandSender sender) {
        sender.sendMessage(color(applyPrefix("&6Running RoyalEconomy debug test...")));

        // 1) Core loaded
        sendCheck(sender, "Core economy manager",
                economy != null);

        // 2) Storage / balances available
        boolean hasBalances = economy != null && economy.getAllBalances() != null;
        sendCheck(sender, "Balance map available",
                hasBalances);

        // 3) Transaction logger
        sendCheck(sender, "Transaction logger",
                plugin.getTransactionLogger() != null);

        // 4) Banks
        boolean banksEnabled = config.getBoolean("banks.enabled", false);
        if (banksEnabled) {
            sendCheck(sender, "Bank manager (banks.enabled = true)",
                    plugin.getBankManager() != null);
        } else {
            sender.sendMessage(color(applyPrefix("&7- &eBanks disabled &7in config (banks.enabled=false)")));
        }

        // 5) Baltop
        boolean baltopEnabled = config.getBoolean("baltop.enabled", true);
        if (baltopEnabled) {
            sendCheck(sender, "Leaderboard manager (baltop.enabled = true)",
                    plugin.getLeaderboardManager() != null);
        } else {
            sender.sendMessage(color(applyPrefix("&7- &eBaltop disabled &7in config (baltop.enabled=false)")));
        }

        // 6) PlaceholderAPI
        boolean papiPresent = (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null);
        sendCheck(sender, "PlaceholderAPI detected",
                papiPresent);

        // 7) Taxes / boosts / interest just config sanity
        if (config.getBoolean("taxes.enabled", false)) {
            sender.sendMessage(color(applyPrefix("&7- &aTaxes enabled&7 in config.")));
        } else {
            sender.sendMessage(color(applyPrefix("&7- &eTaxes disabled&7 in config.")));
        }

        if (config.getBoolean("boosts.enabled", false)) {
            sender.sendMessage(color(applyPrefix("&7- &aBoosts enabled&7 in config.")));
        } else {
            sender.sendMessage(color(applyPrefix("&7- &eBoosts disabled&7 in config.")));
        }

        if (config.getBoolean("interest.enabled", false)) {
            sender.sendMessage(color(applyPrefix("&7- &aInterest enabled&7 in config.")));
        } else {
            sender.sendMessage(color(applyPrefix("&7- &eInterest disabled&7 in config.")));
        }

        sender.sendMessage(color(applyPrefix("&aDebug test finished.")));
    }

    private void sendCheck(CommandSender sender, String name, boolean ok) {
        if (ok) {
            sender.sendMessage(color(applyPrefix("&7- &a\u2714 " + name)));
        } else {
            sender.sendMessage(color(applyPrefix("&7- &c\u2716 " + name)));
        }
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
