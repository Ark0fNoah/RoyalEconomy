package com.ArkOfNoah.RoyalEconomy.vault;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

public class RoyalEconomyVaultBridge implements net.milkbowl.vault2.economy.Economy {

    private final RoyalEconomy plugin;
    private final Economy royalEconomy; // your API

    public RoyalEconomyVaultBridge(RoyalEconomy plugin, Economy royalEconomy) {
        this.plugin = plugin;
        this.royalEconomy = royalEconomy;
    }

    // ─────────────────────────────────────────
    // Basic info
    // ─────────────────────────────────────────
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    public @NotNull String getName() {
        return "RoyalEconomy";
    }

    public boolean hasSharedAccountSupport() {
        // We are NOT wiring shared accounts via Vault2 – we have our own /bank system.
        return false;
    }

    public boolean hasMultiCurrencySupport() {
        // Single currency only for now
        return false;
    }

    // ─────────────────────────────────────────
    // Currency meta
    // ─────────────────────────────────────────
    @Override
    public int fractionalDigits(@NotNull String pluginName) {
        // You use normal double balances with 2 decimals
        return 2;
    }

    @Override
    @Deprecated
    public @NotNull String format(@NotNull BigDecimal amount) {
        return royalEconomy.format(amount.doubleValue());
    }

    @Override
    @Deprecated
    public @NotNull String format(@NotNull BigDecimal amount, @NotNull String currency) {
        return royalEconomy.format(amount.doubleValue());
    }

    @Override
    public @NotNull String format(@NotNull String pluginName,
                                  @NotNull BigDecimal amount) {
        return royalEconomy.format(amount.doubleValue());
    }

    @Override
    public @NotNull String format(@NotNull String pluginName,
                                  @NotNull BigDecimal amount,
                                  @NotNull String currency) {
        return royalEconomy.format(amount.doubleValue());
    }

    @Override
    public boolean hasCurrency(@NotNull String currency) {
        String id = getDefaultCurrency("RoyalEconomy");
        return currency.equalsIgnoreCase(id);
    }

    @Override
    public @NotNull String getDefaultCurrency(@NotNull String pluginName) {
        // You can make these configurable; for now keep it simple
        return plugin.getConfig().getString("currency.id", "coins");
    }

    @Override
    public @NotNull String defaultCurrencyNamePlural(@NotNull String pluginName) {
        return plugin.getConfig().getString("currency.name-plural", "coins");
    }

    @Override
    public @NotNull String defaultCurrencyNameSingular(@NotNull String pluginName) {
        return plugin.getConfig().getString("currency.name-singular", "coin");
    }

    @Override
    public @NotNull Collection<String> currencies() {
        // Single currency
        return Collections.singleton(getDefaultCurrency("RoyalEconomy"));
    }

    // ─────────────────────────────────────────
    // Account creation / presence
    // ─────────────────────────────────────────
    @Override
    @Deprecated
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name) {
        // Just ensure a balance entry exists
        royalEconomy.setBalance(accountID, royalEconomy.getBalance(accountID));
        return true;
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID,
                                 @NotNull String name,
                                 boolean player) {
        royalEconomy.setBalance(accountID, royalEconomy.getBalance(accountID));
        return true;
    }

    @Override
    @Deprecated
    public boolean createAccount(@NotNull UUID accountID,
                                 @NotNull String name,
                                 @NotNull String worldName) {
        return createAccount(accountID, name);
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID,
                                 @NotNull String name,
                                 @NotNull String worldName,
                                 boolean player) {
        return createAccount(accountID, name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public @NotNull Map getUUIDNameMap() {
        // You don't track names for all accounts → just give an empty map
        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> getAccountName(@NotNull UUID accountID) {
        // You don't persist names in your economy layer → leave empty
        return Optional.empty();
    }

    @Override
    public boolean hasAccount(@NotNull UUID accountID) {
        return plugin.getEconomy().getAllBalances().containsKey(accountID);
    }

    @Override
    public boolean hasAccount(@NotNull UUID accountID,
                              @NotNull String worldName) {
        return hasAccount(accountID);
    }

    @Override
    public boolean renameAccount(@NotNull UUID accountID,
                                 @NotNull String name) {
        // Not stored anywhere by RoyalEconomy
        return false;
    }

    @Override
    public boolean renameAccount(@NotNull String plugin,
                                 @NotNull UUID accountID,
                                 @NotNull String name) {
        return false;
    }

    @Override
    public boolean deleteAccount(@NotNull String pluginName,
                                 @NotNull UUID accountID) {
        // Reset to default balance instead of real delete
        double def = plugin.getConfig().getDouble("default-balance", 0.0);
        royalEconomy.setBalance(accountID, def);
        return true;
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String plugin,
                                           @NotNull UUID accountID,
                                           @NotNull String currency) {
        return hasCurrency(currency);
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String plugin,
                                           @NotNull UUID accountID,
                                           @NotNull String currency,
                                           @NotNull String world) {
        return hasCurrency(currency);
    }

    // ─────────────────────────────────────────
    // Balance
    // ─────────────────────────────────────────
    @Override
    @Deprecated
    public @NotNull BigDecimal getBalance(@NotNull String pluginName,
                                          @NotNull UUID accountID) {
        return BigDecimal.valueOf(royalEconomy.getBalance(accountID));
    }

    @Override
    @Deprecated
    public @NotNull BigDecimal getBalance(@NotNull String pluginName,
                                          @NotNull UUID accountID,
                                          @NotNull String world) {
        return getBalance(pluginName, accountID);
    }

    @Override
    @Deprecated
    public @NotNull BigDecimal getBalance(@NotNull String pluginName,
                                          @NotNull UUID accountID,
                                          @NotNull String world,
                                          @NotNull String currency) {
        return getBalance(pluginName, accountID);
    }

    @Override
    public boolean has(@NotNull String pluginName,
                       @NotNull UUID accountID,
                       @NotNull BigDecimal amount) {
        return royalEconomy.has(accountID, amount.doubleValue());
    }

    @Override
    public boolean has(@NotNull String pluginName,
                       @NotNull UUID accountID,
                       @NotNull String worldName,
                       @NotNull BigDecimal amount) {
        return has(pluginName, accountID, amount);
    }

    @Override
    public boolean has(@NotNull String pluginName,
                       @NotNull UUID accountID,
                       @NotNull String worldName,
                       @NotNull String currency,
                       @NotNull BigDecimal amount) {
        return has(pluginName, accountID, amount);
    }

    // ─────────────────────────────────────────
    // Withdraw / Deposit
    // ─────────────────────────────────────────
    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName,
                                             @NotNull UUID accountID,
                                             @NotNull BigDecimal amount) {
        double amt = amount.doubleValue();
        if (amt < 0) {
            return new EconomyResponse(
                    amount,
                    getBalance(pluginName, accountID),
                    EconomyResponse.ResponseType.FAILURE,
                    "Negative withdrawals are not allowed."
            );
        }

        boolean ok = royalEconomy.withdraw(accountID, amt);
        BigDecimal newBal = getBalance(pluginName, accountID);

        return new EconomyResponse(
                amount,
                newBal,
                ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                ok ? "" : "Withdraw failed"
        );
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName,
                                             @NotNull UUID accountID,
                                             @NotNull String worldName,
                                             @NotNull BigDecimal amount) {
        return withdraw(pluginName, accountID, amount);
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName,
                                             @NotNull UUID accountID,
                                             @NotNull String worldName,
                                             @NotNull String currency,
                                             @NotNull BigDecimal amount) {
        return withdraw(pluginName, accountID, amount);
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName,
                                            @NotNull UUID accountID,
                                            @NotNull BigDecimal amount) {
        double amt = amount.doubleValue();
        if (amt < 0) {
            return new EconomyResponse(
                    amount,
                    getBalance(pluginName, accountID),
                    EconomyResponse.ResponseType.FAILURE,
                    "Negative deposits are not allowed."
            );
        }

        boolean ok = royalEconomy.deposit(accountID, amt);
        BigDecimal newBal = getBalance(pluginName, accountID);

        return new EconomyResponse(
                amount,
                newBal,
                ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                ok ? "" : "Deposit failed"
        );
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName,
                                            @NotNull UUID accountID,
                                            @NotNull String worldName,
                                            @NotNull BigDecimal amount) {
        return deposit(pluginName, accountID, amount);
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName,
                                            @NotNull UUID accountID,
                                            @NotNull String worldName,
                                            @NotNull String currency,
                                            @NotNull BigDecimal amount) {
        return deposit(pluginName, accountID, amount);
    }

    // ─────────────────────────────────────────
    // Shared-account API → we don’t support them
    // ─────────────────────────────────────────
    @Override
    public boolean createSharedAccount(@NotNull String pluginName,
                                       @NotNull UUID accountID,
                                       @NotNull String name,
                                       @NotNull UUID owner) {
        return false;
    }

    @Override
    public boolean isAccountOwner(@NotNull String pluginName,
                                  @NotNull UUID accountID,
                                  @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean setOwner(@NotNull String pluginName,
                            @NotNull UUID accountID,
                            @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean isAccountMember(@NotNull String pluginName,
                                   @NotNull UUID accountID,
                                   @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull UUID uuid,
                                    @NotNull AccountPermission... initialPermissions) {
        return false;
    }

    @Override
    public boolean removeAccountMember(@NotNull String pluginName,
                                       @NotNull UUID accountID,
                                       @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean hasAccountPermission(@NotNull String pluginName,
                                        @NotNull UUID accountID,
                                        @NotNull UUID uuid,
                                        @NotNull AccountPermission permission) {
        return false;
    }

    @Override
    public boolean updateAccountPermission(@NotNull String pluginName,
                                           @NotNull UUID accountID,
                                           @NotNull UUID uuid,
                                           @NotNull AccountPermission permission,
                                           boolean value) {
        return false;
    }
}
