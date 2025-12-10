package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class InterestTask extends BukkitRunnable {

    private final RoyalEconomy plugin;
    private final Economy economy;
    private final BankManager bankManager;
    private final BoostManager boostManager;

    public InterestTask(RoyalEconomy plugin,
                        Economy economy,
                        BankManager bankManager,
                        BoostManager boostManager) {

        this.plugin = plugin;
        this.economy = economy;
        this.bankManager = bankManager;
        this.boostManager = boostManager;
    }

    @Override
    public void run() {
        FileConfiguration cfg = plugin.getConfig();

        if (!cfg.getBoolean("interest.enabled", false)) {
            return;
        }

        handlePlayerInterest(cfg);
        handleBankInterest(cfg);
    }

    // ─────────────────────────────────────────
    // Player interest
    // ─────────────────────────────────────────
    private void handlePlayerInterest(FileConfiguration cfg) {
        if (!cfg.getBoolean("interest.players.enabled", false)) {
            return;
        }

        double rate = cfg.getDouble("interest.players.rate", 0.0);
        if (rate <= 0) return;

        double maxBalance = cfg.getDouble("interest.players.max-balance", -1);
        boolean notify = cfg.getBoolean("interest.players.messages.notify", false);
        boolean onlyOnlineNotify = cfg.getBoolean("interest.players.messages.only-online", true);

        Map<UUID, Double> all = economy.getAllBalances();
        if (all == null || all.isEmpty()) return;

        for (Map.Entry<UUID, Double> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            double bal = entry.getValue();
            if (bal <= 0) continue;

            double baseInterest = bal * rate;

            // Apply boost if present
            double interest = baseInterest;
            if (boostManager != null) {
                interest = boostManager.applyBoost(uuid, baseInterest, "INTEREST_PLAYER");
            }

            if (interest <= 0) continue;

            double newBal = bal + interest;
            if (maxBalance > 0 && newBal > maxBalance) {
                newBal = maxBalance;
                interest = maxBalance - bal;
                if (interest <= 0) continue;
            }

            // Apply
            economy.setBalance(uuid, newBal);

            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : uuid.toString();

            // Log
            plugin.getTransactionLogger().log(
                    "SERVER_INTEREST",
                    name,
                    interest,
                    "INTEREST_PLAYER",
                    true
            );

            // Notify
            if (notify) {
                Player online = op.getPlayer();
                if (!onlyOnlineNotify || (online != null && online.isOnline())) {
                    String msg = cfg.getString(
                            "messages.interest-player-gain",
                            "%prefix% &aYou received &e%interest_formatted% &ain interest. New balance: &a%balance_formatted%&a."
                    );
                    msg = msg
                            .replace("%interest_formatted%", economy.format(interest))
                            .replace("%balance_formatted%", economy.format(newBal));
                    msg = applyPrefix(msg);
                    if (online != null && online.isOnline()) {
                        online.sendMessage(color(msg));
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // Bank interest
    // ─────────────────────────────────────────
    private void handleBankInterest(FileConfiguration cfg) {
        if (bankManager == null) return;
        if (!cfg.getBoolean("interest.banks.enabled", false)) {
            return;
        }

        double rate = cfg.getDouble("interest.banks.rate", 0.0);
        if (rate <= 0) return;

        double maxBalance = cfg.getDouble("interest.banks.max-balance", -1);
        boolean notifyOwner = cfg.getBoolean("interest.banks.messages.notify-owner", false);

        for (BankManager.Bank bank : bankManager.getAllBanks()) {
            double bal = bank.getBalance();
            if (bal <= 0) continue;

            double interest = bal * rate;
            double newBal = bal + interest;

            if (maxBalance > 0 && newBal > maxBalance) {
                newBal = maxBalance;
                interest = maxBalance - bal;
                if (interest <= 0) continue;
            }

            bank.setBalance(newBal);

            // Log
            plugin.getTransactionLogger().log(
                    "SERVER_INTEREST",
                    "BANK:" + bank.getName(),
                    interest,
                    "INTEREST_BANK",
                    true
            );

            // Notify owner (if online and enabled)
            if (notifyOwner) {
                UUID ownerId = bank.getOwner();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
                Player online = owner.getPlayer();
                if (online != null && online.isOnline()) {
                    String msg = cfg.getString(
                            "messages.interest-bank-owner-gain",
                            "%prefix% &aYour bank &e%bank_name% &areceived &e%interest_formatted% &ain interest."
                    );
                    msg = msg
                            .replace("%bank_name%", bank.getName())
                            .replace("%interest_formatted%",
                                    plugin.getEconomy().format(interest));
                    msg = applyPrefix(msg);
                    online.sendMessage(color(msg));
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private String getPrefix() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("messages.use-prefix", true)) return "";
        return cfg.getString("messages.prefix", "&8[&6RoyalEconomy&8]&r ");
    }

    private String applyPrefix(String msg) {
        return msg == null ? "" : msg.replace("%prefix%", getPrefix());
    }

    private String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
