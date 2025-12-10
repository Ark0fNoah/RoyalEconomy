package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import org.bukkit.Bukkit;

import java.util.UUID;

public class TaxManager {

    public enum SinkType {
        VOID,
        ACCOUNT
    }

    private final RoyalEconomy plugin;
    private final Economy economy;

    public TaxManager(RoyalEconomy plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    private double getRate(String path) {
        return plugin.getConfig().getDouble("taxes." + path + ".rate", 0.0);
    }

    private boolean isEnabled(String path) {
        if (!plugin.getConfig().getBoolean("taxes.enabled", false)) return false;
        return plugin.getConfig().getBoolean("taxes." + path + ".enabled", false);
    }

    private SinkType getSinkType(String path) {
        String raw = plugin.getConfig().getString("taxes." + path + ".sink-type", "void");
        return raw.equalsIgnoreCase("account") ? SinkType.ACCOUNT : SinkType.VOID;
    }

    private UUID getSinkAccount(String path) {
        String raw = plugin.getConfig().getString("taxes." + path + ".sink-account-uuid", "");
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sink-account-uuid for taxes." + path + ": " + raw);
            return null;
        }
    }

    /**
     * Apply tax on a base amount for a specific tax type (e.g. "pay", "bank-withdraw").
     *
     * @return netAmount after tax (what the receiver should get).
     */
    public double applyTax(String type, double baseAmount, UUID payer, UUID receiver) {
        if (!isEnabled(type) || baseAmount <= 0) {
            return baseAmount;
        }

        double rate = getRate(type);
        if (rate <= 0) return baseAmount;

        double tax = baseAmount * rate;
        double net = baseAmount - tax;
        if (net < 0) net = 0;

        SinkType sinkType = getSinkType(type);
        UUID sinkAccount = getSinkAccount(type);

        // Handle where the tax goes
        switch (sinkType) {
            case ACCOUNT -> {
                if (sinkAccount == null) {
                    plugin.getLogger().warning("Tax sink ACCOUNT enabled but no valid UUID set for type " + type);
                    // If invalid, just void it
                } else {
                    economy.deposit(sinkAccount, tax);
                }
            }
            case VOID -> {
                // do nothing; money disappears
            }
        }

        // Optional logging
        String payerName = payer != null ? Bukkit.getOfflinePlayer(payer).getName() : "-";
        String receiverName = receiver != null ? Bukkit.getOfflinePlayer(receiver).getName() : "-";

        plugin.getTransactionLogger().log(
                payerName,
                receiverName,
                tax,
                "TAX_" + type.toUpperCase(),
                true
        );

        return net;
    }
}
