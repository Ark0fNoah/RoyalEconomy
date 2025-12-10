package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class EcoAdminCommand implements CommandExecutor {

    private final RoyalEconomy plugin;
    private final EconomyManager economy;
    private final FileConfiguration config;

    public EcoAdminCommand(RoyalEconomy plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = plugin.getConfig();
    }

    // ─────────────────────────────────────────
    // /eco boost ...
    // ─────────────────────────────────────────
    private void handleBoost(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // /eco boost <player>  (preview)
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = target.getUniqueId();

            double effective = plugin.getBoostManager().getMultiplier(uuid);
            long remainingMs = plugin.getBoostManager().getRemainingMillis(uuid);

            String remainingStr;
            if (remainingMs <= 0) {
                remainingStr = "none";
            } else {
                remainingStr = formatDuration(remainingMs);
            }

            String msgKey = (remainingMs <= 0 ? "messages.boost-none" : "messages.boost-info");
            String msg = config.getString(msgKey,
                    "%prefix% &e%player%&7's effective multiplier: &ex%multiplier%&7. Remaining boost: &e%remaining%&7.");

            msg = msg
                    .replace("%player%", targetName)
                    .replace("%multiplier%", String.format(java.util.Locale.US, "%.2f", effective))
                    .replace("%remaining%", remainingStr);

            sender.sendMessage(color(applyPrefix(msg)));
            return;
        }

        if (args.length == 4) {
            // /eco boost <player> <multiplier> <duration>
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = target.getUniqueId();

            String multRaw = args[2];
            double mult = parseMultiplier(multRaw);
            if (mult <= 0) {
                sender.sendMessage(color(applyPrefix("&cInvalid multiplier. Use e.g. 2x or 1.5")));
                return;
            }

            String durationRaw = args[3];
            long millis = parseDuration(durationRaw);
            if (millis <= 0) {
                sender.sendMessage(color(applyPrefix("&cInvalid duration. Use e.g. 30m, 2h, 1d")));
                return;
            }

            plugin.getBoostManager().setTemporaryBoost(uuid, mult, millis);

            String durationStr = formatDuration(millis);

            String msg = config.getString("messages.boost-applied",
                    "%prefix% &aApplied a &ex%multiplier% &aboost to &e%player% &afor &e%duration%&a.");
            msg = msg
                    .replace("%player%", targetName)
                    .replace("%multiplier%", String.format(java.util.Locale.US, "%.2f", mult))
                    .replace("%duration%", durationStr);

            sender.sendMessage(color(applyPrefix(msg)));
            return;
        }

        // usage
        sender.sendMessage(color(applyPrefix("&cUsage: /eco boost <player>")));
        sender.sendMessage(color(applyPrefix("&cUsage: /eco boost <player> <multiplier> <duration>")));
        sender.sendMessage(color(applyPrefix("&7Example: /eco boost Steve 2x 24h")));
    }

    // ─────────────────────────────────────────
    // Command entry point
    // ─────────────────────────────────────────
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("royaleconomy.admin")) {
            sender.sendMessage(color(applyPrefix(config.getString(
                    "messages.no-permission",
                    "%prefix% &cYou don't have permission to do that."
            ))));
            return true;
        }

        // /eco boost ...
        if (args.length >= 1 && args[0].equalsIgnoreCase("boost")) {
            handleBoost(sender, args);
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

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private String getPrefix() {
        if (!config.getBoolean("messages.use-prefix", true)) return "";
        return config.getString("messages.prefix", "&8[&6RoyalEconomy&8]&r ");
    }

    private double parseMultiplier(String raw) {
        raw = raw.toLowerCase().trim();
        if (raw.endsWith("x")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return -1.0; // invalid
        }
    }

    private long parseDuration(String raw) {
        raw = raw.toLowerCase().trim();
        if (raw.length() < 2) return -1L;

        char unit = raw.charAt(raw.length() - 1);
        String numPart = raw.substring(0, raw.length() - 1);

        long base;
        try {
            base = Long.parseLong(numPart);
        } catch (NumberFormatException e) {
            return -1L;
        }

        long millis;
        switch (unit) {
            case 's' -> millis = base * 1000L;
            case 'm' -> millis = base * 60_000L;
            case 'h' -> millis = base * 60L * 60_000L;
            case 'd' -> millis = base * 24L * 60L * 60_000L;
            default -> {
                return -1L;
            }
        }
        return millis;
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 && sb.length() == 0) sb.append(seconds).append("s");

        String out = sb.toString().trim();
        return out.isEmpty() ? "0s" : out;
    }

    private String applyPrefix(String msg) {
        return msg == null ? "" : msg.replace("%prefix%", getPrefix());
    }

    private String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
