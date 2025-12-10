package com.ArkOfNoah.RoyalEconomy.placeholder;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;


public class RoyalEconomyExpansion extends PlaceholderExpansion {

    private final RoyalEconomy plugin;
    private final Economy economy;

    public RoyalEconomyExpansion(RoyalEconomy plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
    }

    // The identifier: %royaleconomy_<placeholder>%
    @Override
    public @NotNull String getIdentifier() {
        return "royaleconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "ArkOfNoah"
                : String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // Keep registered after reloads
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * Main placeholder handler.
     *
     * Placeholders:
     * %royaleconomy_balance%
     * %royaleconomy_balance_formatted%
     * %royaleconomy_top_1_name%
     * %royaleconomy_top_1_balance%
     * %royaleconomy_interest_rate%
     * %royaleconomy_tax_pay%
     */
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String identifier) {

        if (economy == null) {
            return "";
        }

        // ---- Player-based placeholders ----

        if (identifier.equalsIgnoreCase("balance")) {
            if (player == null) return "";
            double balance = economy.getBalance(player.getUniqueId());
            return String.valueOf(balance);
        }

        if (identifier.equalsIgnoreCase("balance_formatted")) {
            if (player == null) return "";
            double balance = economy.getBalance(player.getUniqueId());
            return economy.format(balance);
        }

        // ---- Global config-based placeholders ----
        if (identifier.equalsIgnoreCase("interest_rate")) {
            double rate = plugin.getConfig().getDouble("interest.players.rate", 0.0);
            double perc = rate * 100.0;
            return String.format(Locale.US, "%.2f%%", perc);
        }

        if (identifier.equalsIgnoreCase("tax_pay")) {
            double rate = plugin.getConfig().getDouble("taxes.pay.rate", 0.0);
            double perc = rate * 100.0;
            return String.format(Locale.US, "%.2f%%", perc);
        }

        // ---- Top-list placeholders ----
        // You requested top_1 specifically, but we can easily support more later.

        if (identifier.equalsIgnoreCase("top_1_name")) {
            Map.Entry<UUID, Double> top = getTopEntry(1);
            if (top == null) return "";
            OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(top.getKey());
            String name = topPlayer.getName();
            return name != null ? name : top.getKey().toString();
        }

        if (identifier.equalsIgnoreCase("top_1_balance")) {
            Map.Entry<UUID, Double> top = getTopEntry(1);
            if (top == null) return "0";
            // Use formatted or raw? You asked for balance, so raw value:
            return String.valueOf(top.getValue());
        }

        if (identifier.equalsIgnoreCase("top_1_balance_formatted")) {
            Map.Entry<UUID, Double> top = getTopEntry(1);
            if (top == null) return economy.format(0.0);
            return economy.format(top.getValue());
        }


        // Unknown placeholder
        return null;
    }

    /**
     * Get the Nth richest player (1-based index).
     */
    private @Nullable Map.Entry<UUID, Double> getTopEntry(int position) {
        if (economy == null || position <= 0) return null;

        Map<UUID, Double> all = economy.getAllBalances();
        if (all.isEmpty()) return null;

        return all.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry<UUID, Double>::getValue).reversed())
                .skip(position - 1L)
                .findFirst()
                .orElse(null);
    }
}
