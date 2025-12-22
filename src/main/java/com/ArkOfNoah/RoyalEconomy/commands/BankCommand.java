package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import com.ArkOfNoah.RoyalEconomy.utils.EcoRank;
import org.bukkit.entity.Player;

public class BankCommand extends RoyalCommand {

    public BankCommand(RoyalEconomy plugin) {
        super(plugin, EcoRank.PLAYER);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            msgRaw(player, "bank.usage");
            return;
        }

        BankManager bm = plugin.getBankManager();
        String sub = args[0].toLowerCase();

        // CREATE
        if (sub.equals("create")) {
            if (bm.hasBankAccount(player.getUniqueId())) {
                msg(player, "bank.already-exists");
                return;
            }
            bm.createBankAccount(player.getUniqueId());
            msg(player, "bank.created");
            return;
        }

        // INFO
        if (sub.equals("info")) {
            if (!bm.hasBankAccount(player.getUniqueId())) {
                msg(player, "bank.not-exists");
                return;
            }
            double bal = bm.getBankBalance(player.getUniqueId());
            int level = bm.getBankLevel(player.getUniqueId());
            double interest = plugin.getConfig().getDouble("banks.levels." + level + ".interest-rate", 0.0);

            msg(player, "bank.info",
                    "%level%", String.valueOf(level),
                    "%balance%", plugin.getEconomy().format(bal),
                    "%interest%", String.valueOf(interest)
            );
            return;
        }

        // DEPOSIT / WITHDRAW
        if (args.length < 2) {
            msgRaw(player, "bank.usage");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            msg(player, "general.invalid-number");
            return;
        }

        if (sub.equals("deposit")) {
            if (!bm.hasBankAccount(player.getUniqueId())) {
                msg(player, "bank.not-exists");
                return;
            }

            if (bm.deposit(player.getUniqueId(), amount)) {
                msg(player, "bank.deposited", "%amount%", plugin.getEconomy().format(amount));
            } else {
                // Usually fails due to wallet balance or bank limit
                // You might want to add specific messages in BankManager returns, but for now:
                msg(player, "pay.insufficient-funds", "%balance%", "Your Wallet", "%amount%", String.valueOf(amount));
            }
        }
        else if (sub.equals("withdraw")) {
            if (!bm.hasBankAccount(player.getUniqueId())) {
                msg(player, "bank.not-exists");
                return;
            }

            if (bm.withdraw(player.getUniqueId(), amount)) {
                msg(player, "bank.withdrawn", "%amount%", plugin.getEconomy().format(amount));
            } else {
                msg(player, "bank.not-exists"); // Or insufficient bank funds
            }
        }
    }
}