package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class InterestTask extends BukkitRunnable {

    private final RoyalEconomy plugin;
    private final EconomyManager economy;
    private final BankManager bankManager;
    private final BoostManager boostManager;

    public InterestTask(RoyalEconomy plugin,
                        EconomyManager economy,
                        BankManager bankManager,
                        BoostManager boostManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.bankManager = bankManager;
        this.boostManager = boostManager;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("interest.enabled", false)) return;

        handlePlayerInterest();
        handleBankInterest();
    }

    // --- Player Wallet Interest ---
    private void handlePlayerInterest() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("interest.players.enabled", false)) return;

        double rate = cfg.getDouble("interest.players.rate", 0.00); // 0.01 = 1%
        if (rate <= 0) return;

        for (Map.Entry<UUID, Double> entry : economy.getAllBalances().entrySet()) {
            UUID uuid = entry.getKey();
            double bal = entry.getValue();
            if (bal <= 0) continue;

            double interest = bal * rate;

            // Apply Boosts
            interest = boostManager.applyBoost(uuid, interest, "PLAYER");

            economy.deposit(uuid, interest);

            // Log & Notify
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.isOnline()) {
                String formattedInterest = economy.format(interest);
                plugin.getMessageManager().get("interest.player-received");
                // Note: Ensure you add "interest.player-received" to messages.yml
                // Or use a direct message for now:
                op.getPlayer().sendMessage(plugin.getMessageManager().getRaw("prefix") + " §aReceived §e" + formattedInterest + " §ain interest.");
            }
        }
    }

    // --- Bank Account Interest ---
    private void handleBankInterest() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("interest.banks.enabled", false)) return;

        // Iterate through UUIDs, not Bank Objects
        for (UUID ownerUUID : bankManager.getBankOwners()) {
            double bal = bankManager.getBankBalance(ownerUUID);
            if (bal <= 0) continue;

            // Get rate based on bank level
            int level = bankManager.getBankLevel(ownerUUID);
            double rate = cfg.getDouble("banks.levels." + level + ".interest-rate", 0.0);
            if (rate <= 0) continue;

            // Calculate (Rate is usually percentage in config, e.g., 1.5%)
            double interest = bal * (rate / 100.0);

            bankManager.addInterest(ownerUUID, interest);

            // Log & Notify
            OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUUID);
            if (op.isOnline()) {
                String formattedInterest = economy.format(interest);
                op.getPlayer().sendMessage(plugin.getMessageManager().getRaw("prefix") + " §aYour bank generated §e" + formattedInterest + " §ain interest.");
            }
        }
    }
}